package trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {

    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val service = TrygdetidService(repository, behandlingKlient, grunnlagKlient, TrygdetidBeregningService)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.kanBeregnes(any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente trygdetid`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns trygdetid(behandlingId)

        val trygdetid = service.hentTrygdetid(behandlingId)

        trygdetid shouldNotBe null

        verify(exactly = 1) { repository.hentTrygdetid(behandlingId) }
    }

    @Test
    fun `skal returnere null hvis trygdetid ikke finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns null

        val trygdetid = service.hentTrygdetid(behandlingId)

        trygdetid shouldBe null

        verify(exactly = 1) { repository.hentTrygdetid(behandlingId) }
    }

    @Test
    fun `skal opprette trygdetid`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { sak } returns sakId
        }
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        every { repository.hentTrygdetid(any()) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.opprettTrygdetid(any(), any()) } returns trygdetid(behandlingId)

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandling.sak
            grunnlagKlient.hentGrunnlag(sakId, saksbehandler)
            repository.opprettTrygdetid(behandling, any())
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid naar det allerede finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns trygdetid(behandlingId)

        runBlocking {
            assertThrows<IllegalArgumentException> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid dersom behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlient.kanBeregnes(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) { behandlingKlient.kanBeregnes(behandlingId, saksbehandler) }
    }

    @Test
    fun `skal lagre nytt trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val trygdetidGrunnlagSlot = slot<TrygdetidGrunnlag>()
        val beregnetTrygdetidSlot = slot<BeregnetTrygdetid>()

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentEnkeltTrygdetidGrunnlag(any()) } returns null
        every { repository.opprettTrygdetidGrunnlag(any(), capture(trygdetidGrunnlagSlot)) } answers {
            trygdetid(
                behandlingId = behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlagSlot.captured)
            )
        }
        every { repository.oppdaterBeregnetTrygdetid(any(), capture(beregnetTrygdetidSlot)) } answers {
            trygdetid(
                behandlingId = behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlagSlot.captured),
                beregnetTrygdetid = beregnetTrygdetidSlot.captured
            )
        }

        runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                saksbehandler,
                trygdetidGrunnlag
            )
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagSlot.captured)
            repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetidSlot.captured)
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal oppdatere trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(kilde = "test")
        val trygdetidGrunnlagSlot = slot<TrygdetidGrunnlag>()
        val beregnetTrygdetidSlot = slot<BeregnetTrygdetid>()

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentEnkeltTrygdetidGrunnlag(any()) } returns trygdetidGrunnlag
        every { repository.oppdaterTrygdetidGrunnlag(any(), capture(trygdetidGrunnlagSlot)) } answers {
            trygdetid(
                behandlingId = behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlagSlot.captured)
            )
        }
        every { repository.oppdaterBeregnetTrygdetid(any(), capture(beregnetTrygdetidSlot)) } answers {
            trygdetid(
                behandlingId = behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlagSlot.captured),
                beregnetTrygdetid = beregnetTrygdetidSlot.captured
            )
        }

        runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                saksbehandler,
                endretTrygdetidGrunnlag
            )
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentEnkeltTrygdetidGrunnlag(endretTrygdetidGrunnlag.id)
            repository.oppdaterTrygdetidGrunnlag(behandlingId, trygdetidGrunnlagSlot.captured)
            repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetidSlot.captured)
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal feile ved lagring av trygdetidsgrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        coEvery { behandlingKlient.kanBeregnes(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                    trygdetidGrunnlag
                )
            }
        }

        coVerify { behandlingKlient.kanBeregnes(behandlingId, saksbehandler) }
    }

    @Test
    fun `skal lagre beregnet trygdetid`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(10)
        every { repository.oppdaterBeregnetTrygdetid(any(), any()) } returns trygdetid(behandlingId, beregnetTrygdetid)
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true

        runBlocking {
            service.lagreBeregnetTrygdetid(
                behandlingId,
                saksbehandler,
                beregnetTrygdetid
            )
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal feile ved lagring av beregnet trygdetid hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(10)
        coEvery { behandlingKlient.kanBeregnes(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreBeregnetTrygdetid(
                    behandlingId,
                    saksbehandler,
                    beregnetTrygdetid
                )
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
        }
    }
}