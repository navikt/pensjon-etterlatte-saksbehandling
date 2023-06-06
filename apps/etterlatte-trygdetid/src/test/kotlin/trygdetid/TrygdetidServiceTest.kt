package trygdetid

import io.kotest.matchers.collections.shouldNotContain
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
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.trygdetid.LandNormalisert
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
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
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
            behandling.behandlingType
        }
    }

    @Test
    fun `skal kopiere trygdetid hvis revurdering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns sakId
            every { behandlingType } returns BehandlingType.REVURDERING
        }
        val forrigebehandlingId = randomUUID()
        val forrigeBehandling = mockk<DetaljertBehandling>().apply {
            every { id } returns forrigebehandlingId
        }
        val trygdetid = trygdetid(behandlingId, sakId)

        every { repository.hentTrygdetid(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns forrigeBehandling
        every { repository.hentTrygdetid(forrigebehandlingId) } returns trygdetid
        every { repository.opprettTrygdetid(any()) } returns trygdetid

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetid(forrigebehandlingId)

            repository.opprettTrygdetid(
                withArg {
                    it.opplysninger shouldBe trygdetid.opplysninger
                }
            )
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            forrigeBehandling.id
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det ikke er regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns sakId
            every { behandlingType } returns BehandlingType.REVURDERING
            every { revurderingsaarsak } returns RevurderingAarsak.SOESKENJUSTERING
        }
        val forrigeBehandlingId = randomUUID()
        val forrigeBehandling = mockk<DetaljertBehandling>().apply {
            every { id } returns forrigeBehandlingId
        }
        val trygdetid = trygdetid(behandlingId, sakId)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetid(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns forrigeBehandling
        every { repository.hentTrygdetid(forrigeBehandlingId) } returns null
        every { repository.opprettTrygdetid(any()) } returns trygdetid

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetid(forrigeBehandlingId)
            grunnlagKlient.hentGrunnlag(any(), any())

            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                }
            )
        }
        verify {
            behandling.revurderingsaarsak
            behandling.id
            behandling.sak
            behandling.behandlingType
            forrigeBehandling.id
        }
    }

    @Test
    fun `skal ikke opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er automatisk regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns sakId
            every { behandlingType } returns BehandlingType.REVURDERING
            every { revurderingsaarsak } returns RevurderingAarsak.REGULERING
            every { prosesstype } returns Prosesstype.AUTOMATISK
        }
        val forrigeBehandlingId = randomUUID()
        val forrigeBehandling = mockk<DetaljertBehandling>().apply {
            every { id } returns forrigeBehandlingId
        }
        every { repository.hentTrygdetid(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns forrigeBehandling
        every { repository.hentTrygdetid(forrigeBehandlingId) } returns null

        runBlocking {
            val err = assertThrows<RuntimeException> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }

            err.message shouldBe "Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt"
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetid(forrigeBehandlingId)
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
            forrigeBehandling.id
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er manuell regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns sakId
            every { behandlingType } returns BehandlingType.REVURDERING
            every { revurderingsaarsak } returns RevurderingAarsak.REGULERING
            every { prosesstype } returns Prosesstype.MANUELL
        }
        val forrigeBehandlingId = randomUUID()
        val forrigeBehandling = mockk<DetaljertBehandling>().apply {
            every { id } returns forrigeBehandlingId
        }
        val trygdetid = trygdetid(behandlingId, sakId)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetid(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns forrigeBehandling
        every { repository.hentTrygdetid(forrigeBehandlingId) } returns null
        every { repository.opprettTrygdetid(any()) } returns trygdetid

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetid(forrigeBehandlingId)
            grunnlagKlient.hentGrunnlag(any(), any())

            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                }
            )
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
            forrigeBehandling.id
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
                trygdetidGrunnlag.copy(bosted = LandNormalisert.POLEN.isoCode)
            )
        }

        with(trygdetid.trygdetidGrunnlag.first()) {
            bosted shouldBe LandNormalisert.POLEN.isoCode
            beregnetTrygdetid shouldBe null
        }
        trygdetid.beregnetTrygdetid shouldBe null

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
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = LandNormalisert.NORGE.isoCode)

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
                        tg.bosted shouldBe LandNormalisert.NORGE.isoCode
                    }
                }
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any())
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal slette trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag))

        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid = runBlocking {
            service.slettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag.id, saksbehandler)
        }

        trygdetid.trygdetidGrunnlag shouldNotContain trygdetidGrunnlag
        trygdetid.beregnetTrygdetid shouldBe null

        coVerify(exactly = 1) {
            behandlingKlient.kanBeregnes(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                }
            )
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
        val forrigeTrygdetidOpplysninger = listOf<Opplysningsgrunnlag>()
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