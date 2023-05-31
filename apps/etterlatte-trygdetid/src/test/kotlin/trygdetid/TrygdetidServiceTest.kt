package trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.trygdetid.Opplysningsgrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidOpplysningType
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Period
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {

    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val beregningService: TrygdetidBeregningService = spyk(TrygdetidBeregningService)
    private val service = TrygdetidService(repository, behandlingKlient, grunnlagKlient, beregningService)

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
            every { id } returns behandlingId
            every { sak } returns sakId
        }
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetFoedselsdato = grunnlag.hentAvdoed().hentFoedselsdato()!!.verdi
        val forventetDoedsdato = grunnlag.hentAvdoed().hentDoedsdato()!!.verdi
        val trygdetid = trygdetid(behandlingId, sakId)

        every { repository.hentTrygdetid(any()) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.opprettTrygdetid(any()) } returns trygdetid

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(sakId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.opprettTrygdetid(
                withArg { trygdetid ->
                    trygdetid.opplysninger.let { opplysninger ->
                        with(opplysninger[0]) {
                            type shouldBe TrygdetidOpplysningType.FOEDSELSDATO
                            opplysning shouldBe forventetFoedselsdato.toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[1]) {
                            type shouldBe TrygdetidOpplysningType.FYLT_16
                            opplysning shouldBe forventetFoedselsdato.plusYears(16).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[2]) {
                            type shouldBe TrygdetidOpplysningType.FYLLER_66
                            opplysning shouldBe forventetFoedselsdato.plusYears(66).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[3]) {
                            type shouldBe TrygdetidOpplysningType.DOEDSDATO
                            opplysning shouldBe forventetDoedsdato!!.toJsonNode()
                            kilde shouldNotBe null
                        }
                    }
                }
            )
        }
        verify {
            behandling.id
            behandling.sak
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
        val eksisterendeTrygdetid = trygdetid(behandlingId)

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid = runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                saksbehandler,
                trygdetidGrunnlag
            )
        }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                }
            )
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any())
        }
    }

    @Test
    fun `Perioder utenfor Norge skal ikke gi trygdetid`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId)

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid = runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                saksbehandler,
                trygdetidGrunnlag.copy(bosted = "Polen")
            )
        }

        with(trygdetid.trygdetidGrunnlag.first()) {
            bosted shouldBe "Polen"
            beregnetTrygdetid shouldBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                }
            )
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any())
        }
    }

    @Test
    fun `skal oppdatere trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag))
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = bostedNorge)

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid = runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                saksbehandler,
                endretTrygdetidGrunnlag
            )
        }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg ->
                        tg.id shouldBe trygdetidGrunnlag.id
                        tg.bosted shouldBe bostedNorge
                    }
                }
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any())
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
    fun `skal opprette ny trygdetid av kopi fra forrige behandling`() {
        val sakId = 123L
        val behandlingId = randomUUID()
        val forrigeBehandlingId = randomUUID()
        val forrigeTrygdetidGrunnlag = trygdetidGrunnlag()
        val forrigeTrygdetidOpplysninger = mockk<List<Opplysningsgrunnlag>>()
        val forrigeTrygdetid = trygdetid(
            behandlingId,
            trygdetidGrunnlag = listOf(forrigeTrygdetidGrunnlag),
            opplysninger = forrigeTrygdetidOpplysninger
        )

        val regulering = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns sakId
        }

        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns regulering
        every { repository.hentTrygdetid(forrigeBehandlingId) } returns forrigeTrygdetid
        every { repository.opprettTrygdetid(any()) } answers { firstArg() }

        runBlocking {
            service.kopierSisteTrygdetidberegning(behandlingId, forrigeBehandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetid(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(match { it.behandlingId == behandlingId })
        }
        verify {
            regulering.id
            regulering.sak
        }
    }
}