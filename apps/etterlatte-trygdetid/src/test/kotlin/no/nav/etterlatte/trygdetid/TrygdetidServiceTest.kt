package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.eldreAvdoedTestopplysningerMap
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlient
import no.nav.etterlatte.trygdetid.klienter.TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon
import no.nav.etterlatte.trygdetid.klienter.VedtaksvurderingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {
    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val beregningService: TrygdetidBeregningService = spyk(TrygdetidBeregningService)
    private val avtaleService = mockk<AvtaleService>()
    private val pesysklient = mockk<PesysKlient>()
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val service: TrygdetidService =
        TrygdetidServiceImpl(
            repository,
            behandlingKlient,
            grunnlagKlient,
            beregningService,
            pesysklient,
            avtaleService,
            vedtaksvurderingKlient,
            DummyFeatureToggleService(),
        )

    private fun trygdeavtale(behandlingId: UUID) =
        Trygdeavtale(
            id = randomUUID(),
            behandlingId = behandlingId,
            avtaleKode = "",
            avtaleDatoKode = "",
            avtaleKriteriaKode = "",
            personKrets = JaNei.JA,
            arbInntekt1G = JaNei.JA,
            arbInntekt1GKommentar = "",
            beregArt50 = JaNei.JA,
            beregArt50Kommentar = "",
            nordiskTrygdeAvtale = JaNei.JA,
            nordiskTrygdeAvtaleKommentar = "",
            kilde = Grunnlagsopplysning.automatiskSaksbehandler,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns true
        coEvery { pesysklient.hentTrygdetidsgrunnlag(any(), any()) } returns TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon(null, null)
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

        coVerify(exactly = 2) {
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
                every { tidligereFamiliepleier } returns null
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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

        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            behandling.tidligereFamiliepleier
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
        val forrigeTrygdetid = trygdetid(behandlingId, sakId)
        val oppdatertTrygdetidCaptured = slot<Trygdetid>()
        val vedtakSammendrag = mockVedtak(forrigebehandlingId, VedtakType.ENDRING)

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigebehandlingId,
            )
        coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns
            listOf(
                vedtakSammendrag,
            )
        every { repository.hentTrygdetiderForBehandling(forrigebehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.opprettTrygdetid(capture(oppdatertTrygdetidCaptured)) } returns forrigeTrygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        val avtale = trygdeavtale(forrigebehandlingId)
        every { avtaleService.hentAvtaleForBehandling(forrigebehandlingId) } returns avtale
        val avtaleSlot = slot<Trygdeavtale>()
        every { avtaleService.opprettAvtale(capture(avtaleSlot)) } just Runs

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }

        avtaleSlot.captured.shouldBeEqualToIgnoringFields(avtale, Trygdeavtale::behandlingId, Trygdeavtale::id)

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigebehandlingId)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            avtaleService.hentAvtaleForBehandling(forrigebehandlingId)
            avtaleService.opprettAvtale(any())

            repository.opprettTrygdetid(oppdatertTrygdetidCaptured.captured)
            with(oppdatertTrygdetidCaptured.captured) {
                this.opplysninger.size shouldBe forrigeTrygdetid.opplysninger.size
                for (i in 0 until this.opplysninger.size) {
                    this.opplysninger[i].opplysning shouldBe forrigeTrygdetid.opplysninger[i].opplysning
                    this.opplysninger[i].kilde shouldBe forrigeTrygdetid.opplysninger[i].kilde
                    this.opplysninger[i].type shouldBe forrigeTrygdetid.opplysninger[i].type
                }
            }
        }
        coVerify {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
        }
        verify {
            behandling.behandlingType
            behandling.sak
            behandling.id
            behandling.revurderingsaarsak
            vedtakSammendrag.behandlingId
            vedtakSammendrag.vedtakType
        }
    }

    @Test
    fun `skal kopiere trygdetid hvis revurdering med flyktningfordel`() {
        val forrigeTrygdetid =
            trygdetid(
                behandlingId = randomUUID(),
                sakId = randomSakId(),
                ident = "UKJENT_AVDOED",
                beregnetTrygdetid = beregnetTrygdetid(overstyrt = true, total = 39),
            )
        val behandlingId = randomUUID()
        val sakId = forrigeTrygdetid.sakId
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.DOEDSFALL
            }
        val grunnlagUtenAvdoede = grunnlagUtenAvdoede()
        val forrigebehandlingId = forrigeTrygdetid.behandlingId
        val vedtakSammendrag = mockVedtak(forrigebehandlingId, VedtakType.ENDRING)

        val oppdatertTrygdetidCaptured = slot<Trygdetid>()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.hentTrygdetiderForBehandling(forrigebehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.hentTrygdetidMedId(forrigebehandlingId, any()) } returns forrigeTrygdetid
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns listOf(vedtakSammendrag)
        every { repository.hentTrygdetiderForBehandling(forrigebehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.opprettTrygdetid(capture(oppdatertTrygdetidCaptured)) } returns forrigeTrygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlagUtenAvdoede
        coEvery { grunnlagKlient.hentGrunnlag(forrigebehandlingId, any()) } returns grunnlagUtenAvdoede
        val avtale = trygdeavtale(forrigebehandlingId)
        every { avtaleService.hentAvtaleForBehandling(forrigebehandlingId) } returns avtale
        val avtaleSlot = slot<Trygdeavtale>()
        every { avtaleService.opprettAvtale(capture(avtaleSlot)) } just Runs

        runBlocking {
            service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler)
        }

        avtaleSlot.captured.shouldBeEqualToIgnoringFields(avtale, Trygdeavtale::id, Trygdeavtale::behandlingId)

        coVerify {
            grunnlagKlient.hentGrunnlag(any(), saksbehandler)
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigebehandlingId)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            avtaleService.hentAvtaleForBehandling(forrigebehandlingId)
            avtaleService.opprettAvtale(any())

            repository.opprettTrygdetid(oppdatertTrygdetidCaptured.captured)
            with(oppdatertTrygdetidCaptured.captured) {
                this.opplysninger.size shouldBe forrigeTrygdetid.opplysninger.size
                for (i in 0 until this.opplysninger.size) {
                    this.opplysninger[i].opplysning shouldBe forrigeTrygdetid.opplysninger[i].opplysning
                    this.opplysninger[i].kilde shouldBe forrigeTrygdetid.opplysninger[i].kilde
                    this.opplysninger[i].type shouldBe forrigeTrygdetid.opplysninger[i].type
                }
            }
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            behandling.revurderingsaarsak
            vedtakSammendrag.behandlingId
            vedtakSammendrag.vedtakType
        }
    }

    @Test
    fun `skal opprette overstyrt trygdetid hvis det ikke finnes avdoede i grunnlag (ukjent avdoed)`() {
        val behandlingId = randomUUID()
        val grunnlagUtenAvdoede = grunnlagUtenAvdoede()
        val behandling =
            mockk<DetaljertBehandling> {
                every { id } returns behandlingId
                every { sak } returns randomSakId()
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { tidligereFamiliepleier } returns null
            }

        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlagUtenAvdoede

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        val opprettTrygdetidCaptured = slot<Trygdetid>()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(capture(opprettTrygdetidCaptured)) } returns mockk(relaxed = true)

        runBlocking { service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler) }

        opprettTrygdetidCaptured.captured.ident shouldBe UKJENT_AVDOED
        opprettTrygdetidCaptured.captured.beregnetTrygdetid
            ?.resultat
            ?.overstyrt shouldBe true

        coVerify {
            grunnlagKlient.hentGrunnlag(any(), saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.opprettTrygdetid(any())
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        verify {
            behandling.id
            behandling.behandlingType
            behandling.status
            behandling.sak
            behandling.tidligereFamiliepleier
        }
    }

    @Test
    fun `skal opprette overstyrt trygdetid hvis det er en tidligere familiepleier sak`() {
        val behandlingId = randomUUID()
        val grunnlagUtenAvdoede = grunnlagUtenAvdoede()
        val behandling =
            mockk<DetaljertBehandling> {
                every { id } returns behandlingId
                every { sak } returns randomSakId()
                every { status } returns BehandlingStatus.VILKAARSVURDERT
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { soeker } returns SOEKER_FOEDSELSNUMMER.value
                every { tidligereFamiliepleier?.svar } returns true
            }

        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlagUtenAvdoede

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        val opprettTrygdetidCaptured = slot<Trygdetid>()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(capture(opprettTrygdetidCaptured)) } returns mockk(relaxed = true)

        runBlocking { service.opprettTrygdetiderForBehandling(behandlingId, saksbehandler) }

        opprettTrygdetidCaptured.captured.ident shouldBe SOEKER_FOEDSELSNUMMER.value
        opprettTrygdetidCaptured.captured.beregnetTrygdetid
            ?.resultat
            ?.overstyrt shouldBe true

        coVerify {
            grunnlagKlient.hentGrunnlag(any(), saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.opprettTrygdetid(any())
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        verify {
            behandling.id
            behandling.behandlingType
            behandling.status
            behandling.sak
            behandling.tidligereFamiliepleier?.svar
            behandling.soeker
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
        val vedtakSammendrag = mockVedtak(forrigeBehandlingId, VedtakType.ENDRING)

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns listOf(vedtakSammendrag)
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
            avtaleService.hentAvtaleForBehandling(any())
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, saksbehandler)
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.id
            behandling.sak
            behandling.behandlingType
            vedtakSammendrag.behandlingId
            vedtakSammendrag.vedtakType
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
        val vedtakSammendrag = mockVedtak(forrigeBehandlingId, VedtakType.ENDRING)

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns listOf(vedtakSammendrag)
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
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, saksbehandler)
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
            vedtakSammendrag.behandlingId
            vedtakSammendrag.vedtakType
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
        val vedtakSammendrag = mockVedtak(forrigeBehandlingId, VedtakType.ENDRING)

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns listOf(vedtakSammendrag)
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

        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
        }

        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            vedtaksvurderingKlient.hentIverksatteVedtak(sakId, saksbehandler)
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
            vedtakSammendrag.behandlingId
            vedtakSammendrag.vedtakType
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
                every { tidligereFamiliepleier } returns null
            }

        val doedsdato = LocalDate.of(2023, 11, 12)
        val foedselsdato = doedsdato.minusYears(30)

        val grunnlag = grunnlagMedEkstraAvdoedForelder(foedselsdato, doedsdato)

        val trygdetid = trygdetid(behandlingId)

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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
            repository.oppdaterTrygdetid(any())
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }
        coVerify(exactly = 2) {
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }
        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
        }

        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            behandling.tidligereFamiliepleier
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
        }

        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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
            avtaleService.hentAvtaleForBehandling(any())
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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
            avtaleService.hentAvtaleForBehandling(any())
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
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
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
            avtaleService.hentAvtaleForBehandling(any())
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            beregningService.beregnTrygdetid(any(), any(), any(), any(), any(), any())
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
            )
        } returns eksisterendeTrygdetid

        service.overstyrBeregnetTrygdetidForAvdoed(
            behandlingId,
            eksisterendeTrygdetid.ident,
            beregnetTrygdetid(25, Tidspunkt.now()).resultat,
        )

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.oppdaterTrygdetid(oppdatertTrygdetidCaptured.captured)
        }
        with(oppdatertTrygdetidCaptured.captured) {
            this.trygdetidGrunnlag.size shouldBe 0
            this.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 25
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
                beregnetTrygdetid = beregnetTrygdetid(overstyrt = true, total = 20),
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
        every { avtaleService.hentAvtaleForBehandling(any()) } returns null

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
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(
                match {
                    it.behandlingId == behandlingId &&
                        it.beregnetTrygdetid?.resultat?.overstyrt == true
                },
            )
        }

        coVerify(exactly = 2) {
            grunnlagKlient.hentGrunnlag(forrigeBehandlingId, saksbehandler)
        }

        verify {
            regulering.id
            regulering.sak

            avtaleService.hentAvtaleForBehandling(forrigeBehandlingId)
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
        every { avtaleService.hentAvtaleForBehandling(any()) } returns null

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
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(match { it.behandlingId == behandlingId })
        }
        coVerify(exactly = 2) {
            grunnlagKlient.hentGrunnlag(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(forrigeBehandlingId, saksbehandler)
        }
        verify {
            revurdering.id
            revurdering.sak

            avtaleService.hentAvtaleForBehandling(forrigeBehandlingId)
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

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { repository.hentTrygdetidMedId(any(), any()) } returns eksisterendeTrygdetid
        coEvery { repository.oppdaterTrygdetid(any()) } returnsArgument 0
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
        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
        }
        verify(exactly = 1) {
            repository.hentTrygdetidMedId(any(), any())
            beregningService.beregnTrygdetid(any(), any(), any(), any(), true, any())
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
        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlag
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { repository.hentTrygdetidMedId(any(), any()) } returns eksisterendeTrygdetid
        coEvery { repository.oppdaterTrygdetid(any()) } returnsArgument 0
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        val trygdetid =
            runBlocking {
                service.overstyrNorskPoengaaarForTrygdetid(eksisterendeTrygdetid.id, behandlingId, 10, saksbehandler)
            }

        trygdetid shouldNotBe null
        trygdetid.overstyrtNorskPoengaar shouldBe 10
        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10

        coVerify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(any())
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any())
        }
        verify(exactly = 1) {
            repository.hentTrygdetidMedId(any(), any())
            beregningService.beregnTrygdetid(any(), any(), any(), 10, any(), any())
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
    fun `opprettOverstyrtBeregnetTrygdetid kaster feil hvis behandling ikke kan endres`() {
        val behandlingId = randomUUID()
        val behandling =
            mockk<DetaljertBehandling> {
                every { status } returns BehandlingStatus.IVERKSATT
            }
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns behandling

        assertThrows<IkkeTillattException> {
            runBlocking { service.opprettOverstyrtBeregnetTrygdetid(behandlingId, true, saksbehandler) }
        }
        assertThrows<IkkeTillattException> {
            runBlocking { service.opprettOverstyrtBeregnetTrygdetid(behandlingId, false, saksbehandler) }
        }
        coVerify(exactly = 2) {
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
        }
        verify { behandling.status }
    }

    @Test
    fun `opprettOverstyrtBeregnetTrygdetid kaster feil hvis ingen avdoede i persongalleri har doedsdato`() {
        val sakId: SakId = randomSakId()
        val behandlingId = randomUUID()
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides =
                    mapOf(
                        Opplysningstype.DOEDSDATO to
                            Opplysning.Konstant(
                                randomUUID(),
                                Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                                objectMapper.valueToTree(null),
                            ),
                    ),
                opplysningsmapSakOverrides =
                    mapOf(
                        Opplysningstype.PERSONGALLERI_V1 to
                            Opplysning.Konstant(
                                randomUUID(),
                                Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                                Persongalleri(
                                    soeker = SOEKER_FOEDSELSNUMMER.value,
                                    avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                                ).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()

        val behandling: DetaljertBehandling =
            mockk {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { status } returns BehandlingStatus.OPPRETTET
            }
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()

        assertThrows<UgyldigForespoerselException> {
            runBlocking {
                service.opprettOverstyrtBeregnetTrygdetid(
                    behandlingId,
                    overskriv = false,
                    saksbehandler,
                )
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
        }
        verify {
            behandling.status
            behandling.id
        }
    }

    @Test
    fun `opprettOverstyrtBeregnetTrygdetid tillater aa opprette overstyrt trygdetid uten avdoede`() {
        val sakId: SakId = randomSakId()
        val behandlingId = randomUUID()
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides = emptyMap(),
                opplysningsmapSakOverrides =
                    mapOf(
                        Opplysningstype.PERSONGALLERI_V1 to
                            Opplysning.Konstant(
                                randomUUID(),
                                Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                                Persongalleri(
                                    soeker = SOEKER_FOEDSELSNUMMER.value,
                                    avdoed = emptyList(),
                                ).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()

        val opprettetTrygdetidSlot = slot<Trygdetid>()
        val behandling: DetaljertBehandling =
            mockk {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { status } returns BehandlingStatus.OPPRETTET
                every { tidligereFamiliepleier } returns null
            }
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(capture(opprettetTrygdetidSlot)) } returnsArgument 0
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0
        runBlocking {
            service.opprettOverstyrtBeregnetTrygdetid(
                behandlingId,
                overskriv = false,
                saksbehandler,
            )
        }

        coVerify(exactly = 1) {
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.opprettTrygdetid(any())
        }
        verify {
            behandling.status
            behandling.id
            behandling.sak
            behandling.tidligereFamiliepleier
        }
    }

    @Test
    fun `opprettOverstyrtBeregnetTrygdetid tillater å opprette overstyrt trygdetid uten avdøde med soeker som ident`() {
        val sakId: SakId = randomSakId()
        val behandlingId = randomUUID()
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides = emptyMap(),
                opplysningsmapSakOverrides =
                    mapOf(
                        Opplysningstype.PERSONGALLERI_V1 to
                            Opplysning.Konstant(
                                randomUUID(),
                                Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                                Persongalleri(
                                    soeker = SOEKER_FOEDSELSNUMMER.value,
                                    avdoed = emptyList(),
                                ).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()

        val opprettetTrygdetidSlot = slot<Trygdetid>()
        val behandling: DetaljertBehandling =
            mockk {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { status } returns BehandlingStatus.OPPRETTET
                every { soeker } returns "12345678901"
                every { tidligereFamiliepleier } returns
                    TidligereFamiliepleier(
                        svar = true,
                        kilde = Grunnlagsopplysning.Saksbehandler("12345678901", Tidspunkt.now()),
                        foedselsnummer = null,
                        startPleieforhold = LocalDate.now().minusYears(1),
                        opphoertPleieforhold = LocalDate.now(),
                        begrunnelse = "Begrunnelse",
                    )
            }
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(behandlingId, any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(capture(opprettetTrygdetidSlot)) } returnsArgument 0
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0
        runBlocking {
            service.opprettOverstyrtBeregnetTrygdetid(
                behandlingId,
                overskriv = false,
                saksbehandler,
            )
        }

        coVerify(exactly = 1) {
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), any())
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.opprettTrygdetid(any())
        }
        verify {
            behandling.status
            behandling.id
            behandling.sak
            behandling.soeker
            behandling.tidligereFamiliepleier
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
                every { tidligereFamiliepleier } returns null
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
            behandling.tidligereFamiliepleier
        }
    }

    @Nested
    inner class BehandlingMedSammeAvdoed {
        private val grunnlagMock = mockk<Grunnlag>()
        private lateinit var avdoedeMocks: List<Grunnlagsdata<JsonNode>>

        private fun setupGrunnlagMock(avdoede: List<Folkeregisteridentifikator>) {
            avdoedeMocks =
                avdoede.map { avdoed ->
                    mockk<Grunnlagsdata<JsonNode>> {
                        every { get(Opplysningstype.FOEDSELSNUMMER) } answers { konstantOpplysning(avdoed) }
                    }
                }
            coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlagMock
            every { grunnlagMock.hentAvdoede() } returns avdoedeMocks
        }

        private fun verifyHentetAvdoedeFraGrunnlag(behandlingId: UUID) {
            coVerify(exactly = 1) {
                grunnlagKlient.hentGrunnlag(behandlingId, any())
                grunnlagMock.hentAvdoede()
            }
            avdoedeMocks.forEach {
                verify(exactly = 1) {
                    it[Opplysningstype.FOEDSELSNUMMER]
                }
            }
        }

        @Test
        fun `finnBehandlingMedTrygdetidForSammeAvdoede skal finne behandling med ok status`() {
            val annenBehandling = behandling(randomUUID(), randomSakId(), BehandlingStatus.BEREGNET)
            val avdoed1 = Folkeregisteridentifikator.of("10418305857")
            val avdoed2 = Folkeregisteridentifikator.of("01448203510")

            setupGrunnlagMock(listOf(avdoed1, avdoed2))
            val behandlingId = randomUUID()

            every { repository.hentTrygdetiderForBehandling(behandlingId) } returns
                listOf(
                    trygdetid(behandlingId, ident = avdoed1.value),
                    trygdetid(behandlingId, ident = avdoed2.value),
                )
            every { repository.hentTrygdetiderForAvdoede(any()) } returns
                listOf(
                    trygdetidPartial(annenBehandling.id, avdoed1),
                    trygdetidPartial(annenBehandling.id, avdoed2),
                )
            coEvery { behandlingKlient.hentBehandling(annenBehandling.id, any()) } returns annenBehandling

            val behandlingMedSammeAvdoede =
                runBlocking {
                    service.finnBehandlingMedTrygdetidForSammeAvdoede(behandlingId, saksbehandler)
                }

            behandlingMedSammeAvdoede shouldBe annenBehandling.id

            verifyHentetAvdoedeFraGrunnlag(behandlingId)
            verify(exactly = 1) {
                repository.hentTrygdetiderForBehandling(behandlingId)
                repository.hentTrygdetiderForAvdoede(listOf(avdoed1.value, avdoed2.value))
            }
            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(annenBehandling.id, saksbehandler)
            }
        }

        @Test
        fun `finnBehandlingMedTrygdetidForSammeAvdoede skal bare ta med behandlinger som har alle avdøde`() {
            val annenBehandling1 = behandling(randomUUID(), randomSakId(), BehandlingStatus.BEREGNET)
            val annenBehandling2 = behandling(randomUUID(), randomSakId(), BehandlingStatus.BEREGNET)
            val behandling = behandling(randomUUID(), randomSakId(), BehandlingStatus.ATTESTERT)
            val avdoed1 = Folkeregisteridentifikator.of("10418305857")
            val avdoed2 = Folkeregisteridentifikator.of("01448203510")

            setupGrunnlagMock(listOf(avdoed1, avdoed2))

            every { repository.hentTrygdetiderForBehandling(behandling.id) } returns
                listOf(
                    trygdetid(behandling.id, ident = avdoed1.value),
                    trygdetid(behandling.id, ident = avdoed2.value),
                )

            every { repository.hentTrygdetiderForAvdoede(any()) } returns
                listOf(
                    trygdetidPartial(annenBehandling1.id, avdoed1),
                    trygdetidPartial(annenBehandling1.id, avdoed2),
                    trygdetidPartial(annenBehandling2.id, avdoed1),
                    trygdetidPartial(behandling.id, avdoed1),
                    trygdetidPartial(behandling.id, avdoed2),
                )
            coEvery { behandlingKlient.hentBehandling(annenBehandling1.id, any()) } returns annenBehandling1

            val behandlingMedSammeAvdoede =
                runBlocking {
                    service.finnBehandlingMedTrygdetidForSammeAvdoede(behandling.id, saksbehandler)
                }
            behandlingMedSammeAvdoede shouldBe annenBehandling1.id

            verifyHentetAvdoedeFraGrunnlag(behandling.id)
            verify(exactly = 1) {
                repository.hentTrygdetiderForBehandling(behandling.id)
                repository.hentTrygdetiderForAvdoede(listOf(avdoed1.value, avdoed2.value))
            }
            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(annenBehandling1.id, saksbehandler)
            }
        }

        @Test
        fun `finnBehandlingMedTrygdetidForSammeAvdoede skal ikke ta med den samme behandlingen`() {
            val annenBehandling = behandling(randomUUID(), randomSakId(), BehandlingStatus.AVBRUTT)
            val behandling = behandling(randomUUID(), randomSakId(), BehandlingStatus.ATTESTERT)
            val avdoed1 = Folkeregisteridentifikator.of("10418305857")
            val avdoed2 = Folkeregisteridentifikator.of("01448203510")

            setupGrunnlagMock(listOf(avdoed1, avdoed2))

            every { repository.hentTrygdetiderForBehandling(behandling.id) } returns
                listOf(
                    trygdetid(behandling.id, ident = avdoed1.value),
                    trygdetid(behandling.id, ident = avdoed2.value),
                )

            every { repository.hentTrygdetiderForAvdoede(any()) } returns
                listOf(
                    trygdetidPartial(annenBehandling.id, avdoed1),
                    trygdetidPartial(annenBehandling.id, avdoed2),
                    trygdetidPartial(behandling.id, avdoed1),
                    trygdetidPartial(behandling.id, avdoed2),
                )
            coEvery { behandlingKlient.hentBehandling(annenBehandling.id, any()) } returns annenBehandling

            val behandlingMedSammeAvdoede =
                runBlocking {
                    service.finnBehandlingMedTrygdetidForSammeAvdoede(behandling.id, saksbehandler)
                }
            behandlingMedSammeAvdoede shouldBe null

            verifyHentetAvdoedeFraGrunnlag(behandling.id)
            verify(exactly = 1) {
                repository.hentTrygdetiderForBehandling(behandling.id)
                repository.hentTrygdetiderForAvdoede(listOf(avdoed1.value, avdoed2.value))
            }
            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(annenBehandling.id, saksbehandler)
            }
        }

        @Test
        fun `finnBehandlingMedTrygdetidForSammeAvdoede skal finne behandling med en avdoed`() {
            val annenBehandling = behandling(randomUUID(), randomSakId(), BehandlingStatus.BEREGNET)
            val avdoed1 = Folkeregisteridentifikator.of("10418305857")

            setupGrunnlagMock(listOf(avdoed1))
            val behandlingId = randomUUID()

            every { repository.hentTrygdetiderForBehandling(behandlingId) } returns
                listOf(trygdetid(behandlingId, ident = avdoed1.value))
            every { repository.hentTrygdetiderForAvdoede(any()) } returns
                listOf(trygdetidPartial(annenBehandling.id, avdoed1))
            coEvery { behandlingKlient.hentBehandling(annenBehandling.id, any()) } returns annenBehandling

            val behandlingMedSammeAvdoede =
                runBlocking {
                    service.finnBehandlingMedTrygdetidForSammeAvdoede(behandlingId, saksbehandler)
                }

            behandlingMedSammeAvdoede shouldBe annenBehandling.id

            verifyHentetAvdoedeFraGrunnlag(behandlingId)
            verify(exactly = 1) {
                repository.hentTrygdetiderForBehandling(behandlingId)
                repository.hentTrygdetiderForAvdoede(listOf(avdoed1.value))
            }
            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(annenBehandling.id, saksbehandler)
            }
        }

        @Test
        fun `kopierTrygdetidsgrunnlag skal feile hvis trygdetidene gjelder forskjellige avdøde`() {
            val behandlingId = randomUUID()
            val kildeBehandlingId = randomUUID()
            every { repository.hentTrygdetiderForBehandling(behandlingId) } returns
                listOf(
                    trygdetid(
                        behandlingId,
                        ident = "01019012345",
                    ),
                )
            every { repository.hentTrygdetiderForBehandling(kildeBehandlingId) } returns
                listOf(
                    trygdetid(behandlingId, ident = "01019012345"),
                    trygdetid(behandlingId, ident = "24128512345"),
                )
            assertThrows<InternfeilException> {
                runBlocking {
                    service.kopierOgOverskrivTrygdetid(behandlingId, kildeBehandlingId, saksbehandler)
                }
            }
            coVerify(exactly = 1) {
                behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
                repository.hentTrygdetiderForBehandling(behandlingId)
                repository.hentTrygdetiderForBehandling(kildeBehandlingId)
            }
        }

        private fun trygdetidPartial(
            behandlingId: UUID,
            avdoed1: Folkeregisteridentifikator,
        ) = TrygdetidPartial(behandlingId, avdoed1.value, Tidspunkt.now())
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

    private fun grunnlagUtenAvdoede(): Grunnlag {
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides = emptyMap(),
                opplysningsmapSakOverrides =
                    mapOf(
                        Opplysningstype.PERSONGALLERI_V1 to
                            Opplysning.Konstant(
                                randomUUID(),
                                Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
                                Persongalleri(
                                    soeker = SOEKER_FOEDSELSNUMMER.value,
                                    avdoed = emptyList(),
                                ).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()
        return grunnlag
    }

    private fun mockVedtak(
        behandlingId: UUID,
        type: VedtakType,
    ) = VedtakSammendragDto(randomUUID().toString(), behandlingId, type, null, null, null, null, null, null)
}
