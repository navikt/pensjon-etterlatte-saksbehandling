package trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {

    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val service = TrygdetidService(repository, behandlingKlient)

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
        every { repository.hentTrygdetid(any()) } returns null
        every { repository.opprettTrygdetid(any()) } returns trygdetid(behandlingId)

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.opprettTrygdetid(behandlingId)
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
        val trygdetid = trygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        every { repository.hentEnkeltTrygdetidGrunnlag(any()) } returns null
        every { repository.opprettTrygdetidGrunnlag(any(), any()) } returns trygdetid(behandlingId)

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
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)
        }
    }

    @Test
    fun `skal oppdatere trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(trygdetid = 5)

        every { repository.hentEnkeltTrygdetidGrunnlag(any()) } returns trygdetidGrunnlag
        every { repository.oppdaterTrygdetidGrunnlag(any(), any()) } returns trygdetid(behandlingId)

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
            repository.oppdaterTrygdetidGrunnlag(behandlingId, endretTrygdetidGrunnlag)
        }
    }

    @Test
    fun `skal feile ved lagring av trygdetidsgrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
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
        val beregnetTrygdetid = beregnetTrygdetid(10, 10, 20)
        every { repository.oppdaterBeregnetTrygdetid(any(), any()) } returns trygdetid(behandlingId, beregnetTrygdetid)

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
        }
    }

    @Test
    fun `skal feile ved lagring av beregnet trygdetid hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(10, 10, 20)
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