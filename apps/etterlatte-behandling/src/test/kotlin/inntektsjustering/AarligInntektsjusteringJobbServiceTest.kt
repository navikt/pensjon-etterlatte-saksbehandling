package no.nav.etterlatte.inntektsjustering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Needed for non-static method source in Kotlin
class AarligInntektsjusteringJobbServiceTest {
    private val omregningService: OmregningService = mockk()
    private val sakService: SakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val grunnlagService: GrunnlagService = mockk()
    private val aldersovergangService: AldersovergangService = mockk()
    private val revurderingService: RevurderingService = mockk()
    private val vedtakKlient: VedtakKlient = mockk()
    private val beregningKlient: BeregningKlient = mockk()
    private val pdlTjenesterKlient: PdlTjenesterKlient = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val rapid: KafkaProdusent<String, String> = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService = mockk()

    val service =
        AarligInntektsjusteringJobbService(
            omregningService,
            sakService,
            behandlingService,
            revurderingService,
            grunnlagService,
            aldersovergangService,
            vedtakKlient,
            beregningKlient,
            pdlTjenesterKlient,
            oppgaveService,
            rapid,
            featureToggleService,
            etteroppgjoerForbehandlingService,
        )

    @BeforeAll
    fun setup() {
        nyKontekstMedBruker(mockk())
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        every { featureToggleService.isEnabled(any(), any()) } returns true

        coEvery { etteroppgjoerForbehandlingService.hentForbehandlinger(any(), any()) } returns emptyList()

        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns loependeYtdelseDto()
        coEvery {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            InntektsjusteringAvkortingInfoResponse(
                SakId(123L),
                aar = 2025,
                harInntektForAar = false,
                harSanksjon = false,
            )
        coEvery { beregningKlient.harOverstyrt(any(), any()) } returns false
        coEvery { aldersovergangService.aldersovergangMaaned(any(), any()) } returns YearMonth.of(2050, 1)
        every { sakService.finnSak(SakId(123L)) } returns gyldigSak
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns emptyList()
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(any()) } returns
            PdlIdentifikator.FolkeregisterIdent(
                Folkeregisteridentifikator.of(fnrGyldigSak),
            )
        coEvery {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                any(),
                any(),
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns personPdl
        every { behandlingService.hentSisteIverksatteBehandling(any()) } returns
            mockk {
                every { id } returns sisteBehandling
                every { utlandstilknytning } returns mockk()
                every { boddEllerArbeidetUtlandet } returns mockk()
                every { opphoerFraOgMed } returns null
            }
        every { grunnlagService.hentPersonopplysninger(any(), any()) } returns
            mockk {
                every { soeker } returns
                    mockk {
                        every { opplysning } returns personGjenny
                    }
            }

        every { grunnlagService.hentPersongalleri(any<SakId>()) } returns persongalleri
        coEvery {
            revurderingService.opprettRevurdering(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            mockk {
                every { oppdater() } returns mockk()
            }
        every { revurderingService.fjernSaksbehandlerFraRevurderingsOppgave(any()) } just runs

        every { omregningService.oppdaterKjoering(any()) } just runs
        every { rapid.publiser(any(), any()) } returns Pair(1, 1L)
    }

    private fun endrePersonopplysninger() =
        listOf(
            personPdl.copy(fornavn = OpplysningDTO("endret fornavn", "")),
            personPdl.copy(mellomnavn = OpplysningDTO("endret mellomnavn ", "")),
            personPdl.copy(etternavn = OpplysningDTO("endret etternavn", "")),
            personPdl.copy(foedselsdato = OpplysningDTO(LocalDate.of(1990, 2, 25), "")),
            personPdl.copy(doedsdato = OpplysningDTO(LocalDate.of(1990, 2, 25), "")),
        )

    @Test
    fun `skal ikke opprette manuell inntektsjustering hvis aapne behandlinger`() {
        val oppgaveId = UUID.randomUUID()
        val sakId = SakId(123L)
        val behandlinger = listOf(mockk<BehandlingOgSak>())
        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns behandlinger

        val exception =
            assertThrows<UgyldigForespoerselException> {
                service.opprettRevurderingForAarligInntektsjustering(sakId, oppgaveId, mockk())
            }
        exception.code shouldBe "KAN_IKKE_OPPRETTE_REVURDERING_PGA_AAPNE_BEHANDLINGER"
    }

    @Test
    fun `skal opprette manuell inntektsjustering og slette oppgave`() {
        val oppgaveId = UUID.randomUUID()
        val sakId = SakId(123L)

        val oppgave = mockk<OppgaveIntern>(relaxed = true)
        every { oppgave.merknad } returns "merknad"

        coEvery { oppgaveService.ferdigstillOppgave(oppgaveId, any()) } returns mockk()
        coEvery { oppgaveService.hentOppgave(oppgaveId) } returns oppgave
        val revurdering = service.opprettRevurderingForAarligInntektsjustering(sakId, oppgaveId, mockk())
        verify {
            revurderingService.opprettRevurdering(
                sakId = sakId,
                persongalleri = any(),
                forrigeBehandling = any(),
                mottattDato = any(),
                prosessType = any(),
                kilde = any(),
                revurderingAarsak = any(),
                virkningstidspunkt = any(),
                begrunnelse = any(),
                saksbehandlerIdent = any(),
                relatertBehandlingId = any(),
                frist = any(),
                paaGrunnAvOppgave = any(),
                opprinnelse = any(),
            )

            oppgaveService.ferdigstillOppgave(oppgaveId, any())
        }

        revurdering shouldNotBe null
    }

    @Test
    fun `starter jobb for gyldig sak`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.KLAR_FOR_OMREGNING
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe null
                    }
                },
            )
        }
        verify {
            rapid.publiser(
                "aarlig-inntektsjustering-123",
                withArg {
                    // TODO
                },
            )
        }
    }

    @Test
    fun `Sak som ikke er loepende skal ferdigstilles`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )
        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns
            loependeYtdelseDto().copy(
                erLoepende = false,
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.FERDIGSTILT
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe "Sak er ikke løpende"
                    }
                },
            )
        }
    }

    @Test
    fun `Sak som allerede har inntekt neste aar skal ferdigstilles`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            InntektsjusteringAvkortingInfoResponse(
                SakId(123L),
                aar = 2025,
                harInntektForAar = true,
                harSanksjon = false,
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.FERDIGSTILT
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe "Sak har allerede oppgitt inntekt for 2025"
                    }
                },
            )
        }
    }

    @Test
    fun `Sak med Etteroppgjoer forbehandling skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        every { etteroppgjoerForbehandlingService.hentForbehandlinger(any(), any()) } returns
            listOf(
                EtteroppgjoerForbehandling.opprett(gyldigSak, mockk(relaxed = true), mockk(), mottattSkatteoppgjoer = true),
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.AAPEN_BEHANDLING.name
                    }
                },
            )
        }
    }

    @Test
    fun `Sak som er under samordning skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery { vedtakKlient.sakHarLopendeVedtakPaaDato(any(), any(), any()) } returns
            loependeYtdelseDto().copy(
                underSamordning = true,
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        // TODO verifer opprettelse rev
        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.TIL_SAMORDNING.name
                    }
                },
            )
        }
    }

    @Test
    fun `Sak som har aapen behandling skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        every { behandlingService.hentAapneBehandlingerForSak(any()) } returns listOf(mockk())

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        // TODO verifer opprettelse rev
        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.AAPEN_BEHANDLING.name
                    }
                },
            )
        }
    }

    @Test
    fun `Sak som har sanksjon skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            InntektsjusteringAvkortingInfoResponse(
                SakId(123L),
                aar = 2025,
                harInntektForAar = false,
                harSanksjon = true,
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.HAR_SANKSJON.name
                    }
                },
            )
        }
    }

    @Test
    fun `Sak hvor ident har endret seg skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        val nyttFnr = Folkeregisteridentifikator.of("22511075258")
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(any()) } returns PdlIdentifikator.FolkeregisterIdent(nyttFnr)

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.UTDATERT_IDENT.name
                    }
                },
            )
        }
    }

    @Test
    fun `sak som har opphoer fom skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        every { behandlingService.hentSisteIverksatteBehandling(any()) } returns
            mockk {
                every { id } returns sisteBehandling
                every { utlandstilknytning } returns mockk()
                every { boddEllerArbeidetUtlandet } returns mockk()
                every { opphoerFraOgMed } returns YearMonth.of(2025, 5)
            }

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.HAR_OPPHOER_FOM.name
                    }
                },
            )
        }
    }

    @Test
    fun `sak som har aldersovergang 67 aar skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery { aldersovergangService.aldersovergangMaaned(any(), any()) } returns YearMonth.of(2025, 3)

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.ALDERSOVERGANG_67.name
                    }
                },
            )
        }
    }

    @Test
    fun `sak som har overstyrt beregning skal gjoeres manuelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery { beregningKlient.harOverstyrt(any(), any()) } returns true

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.HAR_OVERSTYRT_BEREGNING.name
                    }
                },
            )
        }
    }

    @ParameterizedTest(
        name = "Sak hvor {0} har endret seg skal gjoeres manuelt",
    )
    @MethodSource("endrePersonopplysninger")
    fun `Sak hvor personopplysninger har endret seg skal gjoeres manuelt`(endretOpplysningPdl: PersonDTO) {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )

        coEvery {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                any(),
                any(),
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns endretOpplysningPdl

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        // TODO verifer opprettelse rev
        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.UTDATERTE_PERSONO_INFO.name
                    }
                },
            )
        }
    }

    @Test
    fun `Sak som har verge eller fremtidsfullmakt skal gjoeres mauelt`() {
        val request =
            AarligInntektsjusteringRequest(
                kjoering = "kjoering",
                loependeFom = YearMonth.of(2025, 1),
                saker = listOf(SakId(123L)),
            )
        val vergemaal =
            VergemaalEllerFremtidsfullmakt(
                embete = null,
                type = null,
                vergeEllerFullmektig = VergeEllerFullmektig(null, null, null, null, null),
                opphoerstidspunkt = null,
            )

        every { grunnlagService.hentPersonopplysninger(any(), any()) } returns
            mockk {
                every { soeker } returns
                    mockk {
                        every { opplysning } returns
                            personGjenny.copy(
                                vergemaalEllerFremtidsfullmakt = listOf(vergemaal),
                            )
                    }
            }

        coEvery {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                any(),
                any(),
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            personPdl.copy(
                vergemaalEllerFremtidsfullmakt =
                    listOf(
                        OpplysningDTO(
                            vergemaal,
                            "",
                        ),
                    ),
            )

        every { omregningService.oppdaterKjoering(any()) } returns mockk()

        runBlocking {
            service.startAarligInntektsjusteringJobb(request)
        }

        // TODO verifer opprettelse rev
        verify {
            omregningService.oppdaterKjoering(
                withArg {
                    with(it) {
                        kjoering shouldBe "kjoering"
                        status shouldBe KjoeringStatus.TIL_MANUELL
                        sakId shouldBe SakId(123L)
                        begrunnelse shouldBe AarligInntektsjusteringAarsakManuell.VERGEMAAL.name
                    }
                },
            )
        }
    }

    companion object {
        val fnrGyldigSak = "10418305857"
        val gyldigSak =
            Sak(
                fnrGyldigSak,
                SakType.OMSTILLINGSSTOENAD,
                SakId(123L),
                Enhetsnummer("1234"),
                null,
                false,
            )
        val personPdl =
            PersonDTO(
                fornavn = OpplysningDTO("fornavn", ""),
                mellomnavn = null,
                etternavn = OpplysningDTO("etternavn", ""),
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(fnrGyldigSak), ""),
                foedselsdato = OpplysningDTO(LocalDate.of(1980, 2, 24), ""),
                foedselsaar = OpplysningDTO(1080, ""),
                foedeland = null,
                doedsdato = OpplysningDTO(LocalDate.of(2024, 2, 24), ""),
                adressebeskyttelse = null,
                bostedsadresse = null,
                deltBostedsadresse = null,
                kontaktadresse = null,
                oppholdsadresse = null,
                sivilstatus = null,
                sivilstand = null,
                statsborgerskap = null,
                pdlStatsborgerskap = null,
                utland = null,
                familieRelasjon = null,
                avdoedesBarn = null,
                vergemaalEllerFremtidsfullmakt = null,
            )

        val personGjenny =
            Person(
                fornavn = "fornavn",
                mellomnavn = null,
                etternavn = "etternavn",
                foedselsnummer = Folkeregisteridentifikator.of(fnrGyldigSak),
                foedselsdato = LocalDate.of(1980, 2, 24),
                foedselsaar = 1080,
                foedeland = null,
                doedsdato = LocalDate.of(2024, 2, 24),
                adressebeskyttelse = null,
                bostedsadresse = null,
                deltBostedsadresse = null,
                kontaktadresse = null,
                oppholdsadresse = null,
                sivilstatus = null,
                sivilstand = null,
                statsborgerskap = null,
                pdlStatsborgerskap = null,
                utland = null,
                familieRelasjon = null,
                avdoedesBarn = null,
                avdoedesBarnUtenIdent = null,
                vergemaalEllerFremtidsfullmakt = null,
            )

        val sisteBehandling = UUID.randomUUID()
        val persongalleri = mockk<Persongalleri>()

        fun loependeYtdelseDto() =
            LoependeYtelseDTO(
                erLoepende = true,
                underSamordning = false,
                dato = LocalDate.of(2024, 6, 1),
            )
    }
}
