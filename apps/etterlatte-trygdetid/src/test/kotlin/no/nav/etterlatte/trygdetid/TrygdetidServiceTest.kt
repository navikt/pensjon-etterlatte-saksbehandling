package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.eldreAvdoedTestopplysningerMap
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Period
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {
    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val beregningService: TrygdetidBeregningService = spyk(TrygdetidBeregningService)
    private val service: TrygdetidService =
        TrygdetidServiceImpl(
            repository,
            behandlingKlient,
            grunnlagKlient,
            beregningService,
            mockk<PesysKlient>(),
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente trygdetid`() {
        val behandlingId = randomUUID()

        coEvery { repository.hentTrygdetiderForBehandling(any()) } returns listOf(trygdetid(behandlingId))
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()

        val trygdetider = runBlocking { service.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        trygdetider shouldNotBe null

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(any(), any())
        }
    }

    @Test
    fun `skal returnere null hvis trygdetid ikke finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetiderForBehandling(any()) } returns emptyList()

        val trygdetider = runBlocking { service.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        trygdetider shouldBe emptyList()

        verify(exactly = 1) { repository.hentTrygdetiderForBehandling(behandlingId) }
    }

    @Test
    fun `skal opprette trygdetid`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }

        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetFoedselsdato =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsdato()!!
                .verdi
        val forventetDoedsdato =
            grunnlag
                .hentAvdoede()
                .first()
                .hentDoedsdato()!!
                .verdi
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)

        every { repository.hentTrygdetiderForBehandling(any()) } returns emptyList() andThen listOf(trygdetid)
        every { repository.hentTrygdetid(any()) } returns trygdetid
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            val opprettetTrygdetider = service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)

            opprettetTrygdetider.first().trygdetidGrunnlag.size shouldBe 1
            opprettetTrygdetider.first().trygdetidGrunnlag[0].type shouldBe TrygdetidType.FREMTIDIG

            opprettetTrygdetider.first().beregnetTrygdetid?.resultat?.prorataBroek?.let {
                it.nevner shouldNotBe 0
            }
        }

        coVerify {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }
        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetidMedId(any(), any())
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
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
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
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.DOEDSFALL
            }

        val forrigebehandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId, sakId)

        val oppdatertTrygdetidCaptured = slot<Trygdetid>()

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigebehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigebehandlingId) } returns listOf(trygdetid)
        every { repository.opprettTrygdetid(capture(oppdatertTrygdetidCaptured)) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigebehandlingId)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)

            repository.opprettTrygdetid(oppdatertTrygdetidCaptured.captured)
            with(oppdatertTrygdetidCaptured.captured) {
                this.opplysninger.size shouldBe trygdetid.opplysninger.size
                for (i in 0 until this.opplysninger.size) {
                    this.opplysninger[i].opplysning shouldBe trygdetid.opplysninger[i].opplysning
                    this.opplysninger[i].kilde shouldBe trygdetid.opplysninger[i].kilde
                    this.opplysninger[i].type shouldBe trygdetid.opplysninger[i].type
                }
            }
        }
        coVerify {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            behandling.revurderingsaarsak
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det ikke er regulering`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.SOESKENJUSTERING
            }
        val forrigeBehandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            repository.hentTrygdetidMedId(any(), any())
            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    @Test
    fun `skal ikke opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er automatisk regulering`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
                every { prosesstype } returns Prosesstype.AUTOMATISK
            }
        val forrigeBehandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag

        runBlocking {
            assertThrows<ManglerForrigeTrygdetidMaaReguleresManuelt> {
                service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
        }
        coVerify {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er manuell regulering`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
                every { prosesstype } returns Prosesstype.MANUELL
            }
        val forrigeBehandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(any(), any())
            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid naar det allerede finnes for behandling`() {
        val behandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        every { repository.hentTrygdetiderForBehandling(any()) } returns listOf(trygdetid(behandlingId))
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag

        runBlocking {
            assertThrows<TrygdetidAlleredeOpprettetException> {
                service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
            }
        }

        coVerify {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
        }
        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal opprette manglende trygdetid ved opprettelse naar det allerede finnes men ikke for alle avdoede`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()

        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }

        val doedsdato = LocalDate.of(2023, 11, 12)
        val foedselsdato = doedsdato.minusYears(30)

        val grunnlag = grunnlagMedEkstraAvdoedForelder(foedselsdato, doedsdato)

        val trygdetid = trygdetid(behandlingId)

        every { repository.hentTrygdetiderForBehandling(any()) } returns listOf(trygdetid)
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns
            grunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        every { repository.hentTrygdetidMedId(behandlingId, any()) } returns trygdetid
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetidMedId(behandlingId, any())
            repository.oppdaterTrygdetid(any(), any())
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)

            repository.opprettTrygdetid(
                withArg { trygdetid ->
                    trygdetid.ident shouldBe AVDOED2_FOEDSELSNUMMER.value
                    trygdetid.sakId shouldBe sakId
                    trygdetid.behandlingId shouldBe behandlingId

                    trygdetid.opplysninger.let { opplysninger ->
                        with(opplysninger[0]) {
                            type shouldBe TrygdetidOpplysningType.FOEDSELSDATO
                            opplysning shouldBe foedselsdato.toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[1]) {
                            type shouldBe TrygdetidOpplysningType.FYLT_16
                            opplysning shouldBe foedselsdato.plusYears(16).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[2]) {
                            type shouldBe TrygdetidOpplysningType.FYLLER_66
                            opplysning shouldBe foedselsdato.plusYears(66).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[3]) {
                            type shouldBe TrygdetidOpplysningType.DOEDSDATO
                            opplysning shouldBe doedsdato!!.toJsonNode()
                            kilde shouldNotBe null
                        }
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }
        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid dersom behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) }
    }

    @Test
    fun `skal lagre nytt trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId, ident = AVDOED_FOEDSELSNUMMER.value)

        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(AVDOED_FOEDSELSNUMMER.value).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                    behandlingId,
                    eksisterendeTrygdetid.id,
                    trygdetidGrunnlag,
                    saksbehandler,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                },
            )
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `skal lagre nytt trygdetidsgrunnlag med overstyrt poengaar`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag =
            trygdetidGrunnlag().copy(
                periode = TrygdetidPeriode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1)),
            )
        val eksisterendeTrygdetid =
            trygdetid(behandlingId, ident = AVDOED_FOEDSELSNUMMER.value).copy(overstyrtNorskPoengaar = 10)

        val grunnlag = mockk<Grunnlag>()

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag

        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                    behandlingId,
                    eksisterendeTrygdetid.id,
                    trygdetidGrunnlag,
                    saksbehandler,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(1, 0, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                },
            )
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `skal oppdatere trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid =
            trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag), ident = AVDOED_FOEDSELSNUMMER.value)
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = LandNormalisert.NORGE.isoCode)

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(AVDOED_FOEDSELSNUMMER.value).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                    behandlingId,
                    eksisterendeTrygdetid.id,
                    endretTrygdetidGrunnlag,
                    saksbehandler,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg ->
                        tg.id shouldBe trygdetidGrunnlag.id
                        tg.bosted shouldBe LandNormalisert.NORGE.isoCode
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal slette trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid =
            trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag), ident = AVDOED_FOEDSELSNUMMER.value)

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(AVDOED_FOEDSELSNUMMER.value).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.slettTrygdetidGrunnlagForTrygdetid(
                    behandlingId,
                    eksisterendeTrygdetid.id,
                    trygdetidGrunnlag.id,
                    saksbehandler,
                )
            }

        trygdetid.trygdetidGrunnlag shouldNotContain trygdetidGrunnlag
        trygdetid.beregnetTrygdetid shouldBe null

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal overstyre beregnet trygdetid og slette grunnlaget`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid =
            trygdetid(
                behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag),
                ident = AVDOED_FOEDSELSNUMMER.value,
                beregnetTrygdetid = beregnetTrygdetid(35, Tidspunkt.now()),
            )
        val oppdatertTrygdetidCaptured = slot<Trygdetid>()
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        coEvery { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)
        coEvery {
            repository.oppdaterTrygdetid(
                capture(oppdatertTrygdetidCaptured),
                true,
            )
        } returns eksisterendeTrygdetid

        service.overstyrBeregnetTrygdetidForAvdoed(
            behandlingId,
            eksisterendeTrygdetid.ident,
            beregnetTrygdetid(25, Tidspunkt.now()).resultat,
        )

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.oppdaterTrygdetid(oppdatertTrygdetidCaptured.captured, true)
        }
        with(oppdatertTrygdetidCaptured.captured) {
            this.trygdetidGrunnlag.size shouldBe 0
            this.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 25
        }
    }

    @Test
    fun `skal kunne reberegne trygdetid uten fremtidig trygdetid grunnlag`() {
        val behandlingId = randomUUID()
        val behandling = behandling(behandlingId)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val trygdetidGrunnlag =
            trygdetidGrunnlag(periode = TrygdetidPeriode(LocalDate.of(2000, 1, 1), LocalDate.of(2009, 12, 31)))
        val fremtidigTrygdetidGrunnlag =
            trygdetidGrunnlag(
                trygdetidType = TrygdetidType.FREMTIDIG,
                periode = TrygdetidPeriode(LocalDate.of(2010, 1, 1), LocalDate.of(2015, 12, 31)),
            )
        val eksisterendeTrygdetid =
            trygdetid(
                behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag, fremtidigTrygdetidGrunnlag),
                ident = AVDOED_FOEDSELSNUMMER.value,
                beregnetTrygdetid = beregnetTrygdetid(35, Tidspunkt.now()),
            )
        val oppdatertTrygdetidCaptured = slot<Trygdetid>()
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        coEvery {
            repository.oppdaterTrygdetid(
                capture(oppdatertTrygdetidCaptured),
                false,
            )
        } returns eksisterendeTrygdetid

        runBlocking {
            service.reberegnUtenFremtidigTrygdetid(behandlingId, eksisterendeTrygdetid.id, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id)
            repository.oppdaterTrygdetid(oppdatertTrygdetidCaptured.captured, false)
        }
        verify(exactly = 1) {
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any())
        }
        with(oppdatertTrygdetidCaptured.captured) {
            this.trygdetidGrunnlag.size shouldBe 1
            this.trygdetidGrunnlag.first().type shouldBe TrygdetidType.FAKTISK
            this.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10
        }
    }

    @Test
    fun `skal feile ved lagring av trygdetidsgrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId)

        coEvery { repository.hentTrygdetid(any()) } returns eksisterendeTrygdetid
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
                    behandlingId,
                    eksisterendeTrygdetid.id,
                    trygdetidGrunnlag,
                    saksbehandler,
                )
            }
        }

        coVerify { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) }
    }

    @Test
    fun `skal opprette ny trygdetid av kopi fra forrige behandling`() {
        val sakId = randomSakId()
        val behandlingId = randomUUID()
        val forrigeBehandlingId = randomUUID()
        val forrigeTrygdetidGrunnlag = trygdetidGrunnlag()
        val forrigeTrygdetidOpplysninger = standardOpplysningsgrunnlag()
        val forrigeTrygdetid =
            trygdetid(
                forrigeBehandlingId,
                trygdetidGrunnlag = listOf(forrigeTrygdetidGrunnlag),
                opplysninger = forrigeTrygdetidOpplysninger,
            )

        val regulering =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
            }

        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns regulering
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.opprettTrygdetid(any()) } answers { firstArg() }
        coEvery {
            grunnlagKlient.hentGrunnlag(
                forrigeBehandlingId,
                saksbehandler,
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()

        runBlocking {
            service.kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(forrigeBehandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(match { it.behandlingId == behandlingId })
        }
        verify {
            regulering.id
            regulering.sak
        }
    }

    @Test
    fun `skal opprette manglende trygdetid av kopi fra forrige behandling`() {
        val sakId = randomSakId()
        val behandlingId = randomUUID()
        val forrigeBehandlingId = randomUUID()
        val forrigeTrygdetidGrunnlag = trygdetidGrunnlag(begrunnelse = "Forrige")
        val forrigeTrygdetidOpplysninger = standardOpplysningsgrunnlag()
        val trygdetidGrunnlag = trygdetidGrunnlag(begrunnelse = "Eksisterende")
        val trygdetidOpplysninger = standardOpplysningsgrunnlag()
        val forrigeTrygdetid =
            trygdetid(
                forrigeBehandlingId,
                trygdetidGrunnlag = listOf(forrigeTrygdetidGrunnlag),
                opplysninger = forrigeTrygdetidOpplysninger,
                ident = AVDOED2_FOEDSELSNUMMER.value,
            )

        val eksisterendeTrygdetid =
            trygdetid(
                behandlingId,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag),
                opplysninger = trygdetidOpplysninger,
            )

        val revurdering =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
            }

        val doedsdato = LocalDate.of(2023, 11, 12)
        val foedselsdato = doedsdato.minusYears(30)

        val nyligAvdoedFoedselsnummer = AVDOED2_FOEDSELSNUMMER
        val nyligAvdoed: Grunnlagsdata<JsonNode> =
            mapOf(
                Opplysningstype.DOEDSDATO to konstantOpplysning(doedsdato),
                Opplysningstype.PERSONROLLE to konstantOpplysning(PersonRolle.AVDOED),
                Opplysningstype.FOEDSELSNUMMER to konstantOpplysning(nyligAvdoedFoedselsnummer),
                Opplysningstype.FOEDSELSDATO to konstantOpplysning(foedselsdato),
            )

        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns revurdering
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.opprettTrygdetid(any()) } answers { firstArg() }
        coEvery {
            grunnlagKlient.hentGrunnlag(
                behandlingId,
                saksbehandler,
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(forrigeBehandlingId, saksbehandler) } returns
            GrunnlagTestData(opplysningsmapAvdoedOverrides = nyligAvdoed).hentOpplysningsgrunnlag()

        runBlocking {
            val trygdetider = service.kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, saksbehandler)

            assertEquals(2, trygdetider.size)
            assertTrue(trygdetider.any { it.ident == AVDOED_FOEDSELSNUMMER.value })
            assertTrue(trygdetider.any { it.ident == AVDOED2_FOEDSELSNUMMER.value })
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(forrigeBehandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(match { it.behandlingId == behandlingId })
        }
        verify {
            revurdering.id
            revurdering.sak
        }
    }

    @Test
    fun `skal oppdater yrkesskade`() {
        val behandlingId = randomUUID()

        val behandling = behandling(behandlingId)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi

        val beregnetTrygdetid =
            DetaljertBeregnetTrygdetid(
                resultat =
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.ofYears(5),
                                antallMaaneder = 5 * 12,
                            ),
                        faktiskTrygdetidTeoretisk = null,
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 5,
                        samletTrygdetidTeoretisk = null,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                tidspunkt = Tidspunkt.now(),
                regelResultat = "".toJsonNode(),
            )

        val eksisterendeTrygdetid =
            trygdetid(
                behandlingId,
                beregnetTrygdetid = beregnetTrygdetid,
                ident = forventetIdent.value,
                trygdetidGrunnlag =
                    listOf(
                        TrygdetidGrunnlag(
                            id = randomUUID(),
                            type = TrygdetidType.FAKTISK,
                            bosted = "",
                            periode = TrygdetidPeriode(fra = LocalDate.now().minusYears(5), til = LocalDate.now()),
                            kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                            beregnetTrygdetid =
                                BeregnetTrygdetidGrunnlag(
                                    verdi = Period.ofYears(5),
                                    tidspunkt = Tidspunkt.now(),
                                    regelResultat = "".toJsonNode(),
                                ),
                            begrunnelse = "",
                            poengInnAar = false,
                            poengUtAar = false,
                            prorata = false,
                        ),
                    ),
            )

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { repository.hentTrygdetidMedId(any(), any()) } returns eksisterendeTrygdetid
        coEvery { repository.oppdaterTrygdetid(any(), any()) } returnsArgument 0
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        val trygdetid =
            runBlocking {
                service.setYrkesskade(eksisterendeTrygdetid.id, behandlingId, true, saksbehandler)
            }

        trygdetid shouldNotBe null
        trygdetid.yrkesskade shouldBe true
        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any())
        }
        verify(exactly = 1) {
            repository.hentTrygdetidMedId(any(), any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), true)
            repository.oppdaterTrygdetid(
                withArg {
                    it.yrkesskade shouldBe true
                    it.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40
                },
            )
        }
    }

    @Test
    fun `skal oppdater overstyrt poengaar`() {
        val behandlingId = randomUUID()

        val behandling = behandling(behandlingId)
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi

        val beregnetTrygdetid =
            DetaljertBeregnetTrygdetid(
                resultat =
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.ofYears(5),
                                antallMaaneder = 5 * 12,
                            ),
                        faktiskTrygdetidTeoretisk = null,
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 5,
                        samletTrygdetidTeoretisk = null,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                tidspunkt = Tidspunkt.now(),
                regelResultat = "".toJsonNode(),
            )

        val eksisterendeTrygdetid =
            trygdetid(
                behandlingId,
                beregnetTrygdetid = beregnetTrygdetid,
                ident = forventetIdent.value,
                trygdetidGrunnlag =
                    listOf(
                        TrygdetidGrunnlag(
                            id = randomUUID(),
                            type = TrygdetidType.FAKTISK,
                            bosted = "",
                            periode = TrygdetidPeriode(fra = LocalDate.now().minusYears(5), til = LocalDate.now()),
                            kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                            beregnetTrygdetid =
                                BeregnetTrygdetidGrunnlag(
                                    verdi = Period.ofYears(5),
                                    tidspunkt = Tidspunkt.now(),
                                    regelResultat = "".toJsonNode(),
                                ),
                            begrunnelse = "",
                            poengInnAar = false,
                            poengUtAar = false,
                            prorata = false,
                        ),
                    ),
            )

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { repository.hentTrygdetidMedId(any(), any()) } returns eksisterendeTrygdetid
        coEvery { repository.oppdaterTrygdetid(any(), any()) } returnsArgument 0
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        val trygdetid =
            runBlocking {
                service.overstyrNorskPoengaaarForTrygdetid(eksisterendeTrygdetid.id, behandlingId, 10, saksbehandler)
            }

        trygdetid shouldNotBe null
        trygdetid.overstyrtNorskPoengaar shouldBe 10
        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any())
        }
        verify(exactly = 1) {
            repository.hentTrygdetidMedId(any(), any())
            beregningService.beregnTrygdetid(any(), any(), any(), 10, any())
            repository.oppdaterTrygdetid(
                withArg {
                    it.overstyrtNorskPoengaar shouldBe 10
                    it.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10
                },
            )
        }
    }

    @Test
    fun `skal sjekke gyldighet og oppdatere status hvis behandlingstatus er VILKAARSVURDERT`() {
        val behandlingId = randomUUID()
        val eksisterendeTrygdetid = trygdetid(behandlingId, beregnetTrygdetid = beregnetTrygdetid())

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            behandling(behandlingId, behandlingStatus = BehandlingStatus.VILKAARSVURDERT)
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)

        runBlocking {
            val oppdatert = service.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, saksbehandler)
            oppdatert shouldBe true
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.hentBehandling(any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
        }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom det ikke finnes noe trygdetid`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            behandling(behandlingId, behandlingStatus = BehandlingStatus.VILKAARSVURDERT)
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()

        runBlocking {
            assertThrows<IngenTrygdetidFunnetForAvdoede> {
                service.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.hentBehandling(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
        }
    }

    @Test
    fun `skal feile ved sjekking av gyldighet dersom det ikke finnes noe beregning i trygdetid`() {
        val behandlingId = randomUUID()
        val eksisterendeTrygdetid = trygdetid(behandlingId, beregnetTrygdetid = null)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns
            behandling(behandlingId, behandlingStatus = BehandlingStatus.VILKAARSVURDERT)
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)

        runBlocking {
            assertThrows<TrygdetidManglerBeregning> {
                service.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.hentBehandling(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
        }
    }

    @Test
    fun `skal ikke opprette fremtidig grunnlag hvis man er for gammel`() {
        val behandlingId = randomUUID()
        val sakId = randomSakId()
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }
        val grunnlag =
            GrunnlagTestData(opplysningsmapAvdoedOverrides = eldreAvdoedTestopplysningerMap).hentOpplysningsgrunnlag()
        val forventetFoedselsdato =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsdato()!!
                .verdi
        val forventetDoedsdato =
            grunnlag
                .hentAvdoede()
                .first()
                .hentDoedsdato()!!
                .verdi
        val forventetIdent =
            grunnlag
                .hentAvdoede()
                .first()
                .hentFoedselsnummer()!!
                .verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)

        every { repository.hentTrygdetiderForBehandling(any()) } returns emptyList() andThen listOf(trygdetid)
        every { repository.hentTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            val opprettetTrygdetid = service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)

            opprettetTrygdetid.first().trygdetidGrunnlag.size shouldBe 0
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
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
                },
            )
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    private fun beregnetTrygdetid() =
        DetaljertBeregnetTrygdetid(
            resultat =
                DetaljertBeregnetTrygdetidResultat(
                    faktiskTrygdetidNorge =
                        FaktiskTrygdetid(
                            periode = Period.ofYears(5),
                            antallMaaneder = 5 * 12,
                        ),
                    faktiskTrygdetidTeoretisk = null,
                    fremtidigTrygdetidNorge = null,
                    fremtidigTrygdetidTeoretisk = null,
                    samletTrygdetidNorge = 5,
                    samletTrygdetidTeoretisk = null,
                    prorataBroek = null,
                    overstyrt = false,
                    yrkesskade = false,
                    beregnetSamletTrygdetidNorge = null,
                ),
            tidspunkt = Tidspunkt.now(),
            regelResultat = "".toJsonNode(),
        )

    private fun <T : Any> konstantOpplysning(a: T): Opplysning.Konstant<JsonNode> {
        val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), "", "")
        return Opplysning.Konstant(randomUUID(), kilde, a.toJsonNode())
    }

    private fun grunnlagMedEkstraAvdoedForelder(
        foedselsdato: LocalDate,
        doedsdato: LocalDate,
    ): Grunnlag {
        val grunnlagEnAvdoed = GrunnlagTestData().hentOpplysningsgrunnlag()
        val nyligAvdoedFoedselsnummer = AVDOED2_FOEDSELSNUMMER
        val nyligAvdoed: Grunnlagsdata<JsonNode> =
            mapOf(
                Opplysningstype.DOEDSDATO to konstantOpplysning(doedsdato),
                Opplysningstype.PERSONROLLE to konstantOpplysning(PersonRolle.AVDOED),
                Opplysningstype.FOEDSELSNUMMER to konstantOpplysning(nyligAvdoedFoedselsnummer),
                Opplysningstype.FOEDSELSDATO to konstantOpplysning(foedselsdato),
            )
        return GrunnlagTestData(
            opplysningsmapAvdoedeOverrides = listOf(nyligAvdoed) + grunnlagEnAvdoed.hentAvdoede(),
        ).hentOpplysningsgrunnlag()
    }
}
