package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.SummertePensjonsgivendeInntekter
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertTrue
import no.nav.etterlatte.libs.common.vedtak.Periode as VedtakPeriode

class EtteroppgjoerServiceTest {
    private val sakId = sakId1

    private class TestContext(
        val sakId: SakId,
    ) {
        val dao: EtteroppgjoerDao = mockk()
        val forbehandlingDao: EtteroppgjoerForbehandlingDao = mockk()
        val sigrunKlient: SigrunKlient = mockk()
        val beregningKlient: BeregningKlient = mockk()
        val behandlingService: BehandlingService = mockk()
        val vedtakKlient: VedtakKlient = mockk()
        val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService = mockk()
        val hendelsesDao = mockk<HendelseDao>()

        val service =
            EtteroppgjoerService(
                dao = dao,
                forbehandlingDao = forbehandlingDao,
                vedtakKlient = vedtakKlient,
                behandlingService = behandlingService,
                beregningKlient = beregningKlient,
                sigrunKlient = sigrunKlient,
                etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
                hendelseDao = hendelsesDao,
            )

        val sisteIverksatteBehandlingId = UUID.randomUUID()

        init {
            every { dao.hentEtteroppgjoerForInntektsaar(sakId, any()) } returns
                mockk {
                    every { status } returns EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
                }

            coEvery { hendelsesDao.etteroppgjoerHendelse(any(), any(), any(), any()) } just Runs
            coEvery { etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(any(), any(), any()) } just Runs
            coEvery { dao.lagreEtteroppgjoer(any()) } returns 1
            every { dao.oppdaterEtteroppgjoerStatus(any(), any(), any()) } returns Unit
            every { dao.oppdaterFerdigstiltForbehandlingId(any(), any(), any()) } returns Unit

            coEvery { beregningKlient.hentSanksjoner(any(), any()) } returns emptyList()
            coEvery { beregningKlient.hentBeregningsgrunnlag(any(), any()) } returns
                mockk {
                    every { institusjonsopphold } returns emptyList()
                }
            coEvery { beregningKlient.hentOverstyrtBeregning(any(), any()) } returns mockk()
            every { behandlingService.hentFoersteDoedsdato(any(), any()) } returns null
            coEvery { vedtakKlient.hentIverksatteVedtak(any(), any()) } returns
                listOf(
                    VedtakSammendragDto(
                        id = UUID.randomUUID().toString(),
                        behandlingId = sisteIverksatteBehandlingId,
                        vedtakType = VedtakType.INNVILGELSE,
                        behandlendeSaksbehandler = "",
                        datoFattet = null,
                        attesterendeSaksbehandler = "",
                        datoAttestert = null,
                        virkningstidspunkt = null,
                        opphoerFraOgMed = null,
                        iverksettelsesTidspunkt = null,
                    ),
                )

            coEvery { behandlingService.hentBehandling(sisteIverksatteBehandlingId) } returns
                foerstegangsbehandling(
                    sakId = sakId,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    status = BehandlingStatus.ATTESTERT,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
                )
        }

        fun ingenEtteroppgjoer() {
            every { dao.hentEtteroppgjoerForInntektsaar(sakId, any()) } returns null
        }

        fun returnerEtteroppgjoer(etteroppgjoer: Etteroppgjoer) {
            every { dao.hentEtteroppgjoerForInntektsaar(sakId, any()) } returns etteroppgjoer
        }

        fun sigrunGirSvar() {
            coEvery {
                sigrunKlient.hentPensjonsgivendeInntekt(
                    any(),
                    any(),
                )
            } returns PensjonsgivendeInntektAarResponse.stub("09498230323", 2024)
        }

        fun sigrunKasterFeil() {
            coEvery {
                sigrunKlient.hentPensjonsgivendeInntekt(
                    any(),
                    any(),
                )
            } throws Exception("Ingen skatteoppgjør funnet :/")
        }
    }

    @ParameterizedTest(name = "skal hente aktivt etteroppgjør med status={0}")
    @EnumSource(
        value = EtteroppgjoerStatus::class,
        names = ["FERDIGSTILT"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal hente aktivt etteroppgjør for sak`(status: EtteroppgjoerStatus) {
        val ctx = TestContext(sakId)

        val resultat =
            ctx.returnerEtteroppgjoer(
                Etteroppgjoer(
                    sakId = sakId,
                    inntektsaar = 2024,
                    status = status,
                ),
            )

        resultat.shouldNotBeNull()
    }

    @Test
    fun `skal opprette etteroppgjør med status MOTTATT_SKATTEOPPGJOER hvis vi får svar fra sigrun`() {
        val ctx = TestContext(sakId)
        ctx.sigrunGirSvar()
        ctx.ingenEtteroppgjoer()

        val foerstegangsBehandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        val etteroppgjoer =
            runBlocking {
                ctx.service.haandterEtteroppgjoerVedFoerstegangsbehandling(
                    behandling = foerstegangsBehandling,
                    inntektsaar = 2023,
                )
            }

        assertEquals(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER, etteroppgjoer.status)
    }

    @Test
    fun `skal opprette etteroppgjør med status VENTER_PAA_SKATTEOPPGJOER hvis vi ikke får svar fra sigrun`() {
        val cx = TestContext(sakId)
        cx.sigrunKasterFeil()
        cx.ingenEtteroppgjoer()

        val foerstegangsBehandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )

        val etteroppgjoer =
            runBlocking {
                cx.service.haandterEtteroppgjoerVedFoerstegangsbehandling(
                    behandling = foerstegangsBehandling,
                    inntektsaar = 2023,
                )
            }

        assertEquals(EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, etteroppgjoer.status)
    }

    @Test
    fun `etteroppgjør settes til VENTER_PAA_SVAR ved etterbetaling eller tilbakekreving`() {
        val ctx = TestContext(sakId)
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = behandling.sak,
                innvilgetPeriode = Periode(YearMonth.now().minusYears(1), null),
                sisteIverksatteBehandling = behandling.id,
            )

        val etteropgjoerResultat = listOf(EtteroppgjoerResultatType.ETTERBETALING, EtteroppgjoerResultatType.TILBAKEKREVING)

        etteropgjoerResultat.forEach { type ->
            val forbehandlingMedresultat =
                forbehandling.copy(
                    etteroppgjoerResultatType = type,
                    status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
                )
            ctx.service.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandlingMedresultat)
            verify {
                ctx.dao.oppdaterEtteroppgjoerStatus(sakId, forbehandlingMedresultat.aar, EtteroppgjoerStatus.VENTER_PAA_SVAR)
            }
            verify { ctx.dao.oppdaterFerdigstiltForbehandlingId(sakId, forbehandlingMedresultat.aar, forbehandlingMedresultat.id) }
        }
    }

    @Test
    fun `etteroppgjør settes til FERDIGSTILT ved ingen endring`() {
        val ctx = TestContext(sakId)
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = behandling.sak,
                innvilgetPeriode = Periode(YearMonth.now().minusYears(1), null),
                sisteIverksatteBehandling = behandling.id,
            )

        val etteroppgjoerResultat =
            listOf(
                EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING,
                EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING,
            )

        etteroppgjoerResultat.forEach { type ->
            val forbehandlingMedResultat =
                forbehandling.copy(
                    etteroppgjoerResultatType = type,
                    status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
                )
            ctx.service.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandlingMedResultat)
            verify {
                ctx.dao.oppdaterEtteroppgjoerStatus(sakId, forbehandlingMedResultat.aar, EtteroppgjoerStatus.FERDIGSTILT)
            }
            verify { ctx.dao.oppdaterFerdigstiltForbehandlingId(sakId, forbehandlingMedResultat.aar, forbehandlingMedResultat.id) }
        }
    }

    @Test
    fun `etteroppgjør kaster feil hvis resultat mangler`() {
        val ctx = TestContext(sakId)
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )
        val forbehandling =
            EtteroppgjoerForbehandling
                .opprett(
                    sak = behandling.sak,
                    innvilgetPeriode = Periode(YearMonth.now().minusYears(1), null),
                    sisteIverksatteBehandling = behandling.id,
                ).copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)

        assertThrows<InternfeilException> {
            ctx.service.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandling)
        }
    }

    @Test
    fun `kan ikke oppdatere status for ferdigstilt etteroppgjør hvis forbehandlingen ikke er ferdigstilt`() {
        val ctx = TestContext(sakId)
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
            )
        val forbehandling =
            EtteroppgjoerForbehandling.opprett(
                sak = behandling.sak,
                innvilgetPeriode = Periode(YearMonth.now().minusYears(1), null),
                sisteIverksatteBehandling = behandling.id,
            )

        assertThrows<IkkeTillattException> {
            ctx.service.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandling)
        }
    }

    @Test
    fun `opprettEtteroppgjoer returnerer nytt etteroppgjør dersom etteroppgjør ikke finnes for år`() {
        val ctx = TestContext(sakId)
        every { ctx.dao.hentEtteroppgjoerForInntektsaar(sakId, 2024) } returns
            null

        val resultat = runBlocking { ctx.service.opprettNyttEtteroppgjoer(sakId, 2024) }

        with(resultat) {
            this.sakId shouldBe ctx.sakId
            this.inntektsaar shouldBe 2024
            this.status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
        }
        coVerify(exactly = 1) { ctx.dao.lagreEtteroppgjoer(any()) }
    }

    @Test
    fun `opprettEtteroppgjoer oppdaterer etteroppgjør men ikke status dersom behandling ikke er påbegynt`() {
        val ctx = TestContext(sakId)
        every { ctx.dao.hentEtteroppgjoerForInntektsaar(sakId, 2024) } returns
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                harUtlandstilsnitt = false,
            )

        coEvery { ctx.behandlingService.hentBehandling(ctx.sisteIverksatteBehandlingId) } returns
            foerstegangsbehandling(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                status = BehandlingStatus.ATTESTERT,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(dato = YearMonth.now().minusYears(1)),
                utlandstilknytning = utlandstilsnitt(),
            )

        val resultat = runBlocking { ctx.service.opprettNyttEtteroppgjoer(sakId, 2024) }

        with(resultat) {
            this.sakId shouldBe ctx.sakId
            this.inntektsaar shouldBe 2024
            this.status shouldBe EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
            this.harUtlandstilsnitt shouldBe true
        }
        coVerify(exactly = 1) { ctx.dao.lagreEtteroppgjoer(any()) }
    }

    @Test
    fun `finnInnvilgedeAarForSak skal fungere naar sak ikke har innvilgede perioder`() {
        val ctx = TestContext(sakId)
        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        coEvery { ctx.vedtakKlient.hentInnvilgedePerioder(sakId, brukerTokenInfo) } returns emptyList()

        val innvilgedeAarForSak = ctx.service.finnInnvilgedeAarForSak(sakId, brukerTokenInfo)
        assertTrue(innvilgedeAarForSak.isEmpty())
    }

    private fun utlandstilsnitt(): Utlandstilknytning =
        Utlandstilknytning(UtlandstilknytningType.UTLANDSTILSNITT, kildeSaksbehandler(), "begrunnelse")

    fun kildeSaksbehandler() = Grunnlagsopplysning.Saksbehandler(ident = "ident", tidspunkt = Tidspunkt(instant = Instant.now()))

    @Test
    fun `haandterSkatteoppgjoerMottatt kaster exception hvis etteroppgjoer er ferdigstilt men mangler sisteFerdigstilteForbehandling`() {
        val ctx = TestContext(sakId)
        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.FERDIGSTILT,
                sisteFerdigstilteForbehandling = null,
            )
        val sak = sak(sakId = sakId)

        assertThrows<InternfeilException> {
            ctx.service.haandterSkatteoppgjoerMottatt(etteroppgjoer, sak)
        }
    }

    @Test
    fun `haandterSkatteoppgjoerMottatt oppretter vurder konsekvens oppgave naar inntekt har endret seg siden ferdigstilt etteroppgjoer`() {
        val ctx = TestContext(sakId)
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()
        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.FERDIGSTILT,
                sisteFerdigstilteForbehandling = forbehandlingId,
            )
        val sak = sak(sakId = sakId)

        every { ctx.forbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId) } returns
            SummertePensjonsgivendeInntekter(loensinntekt = 500_000, naeringsinntekt = 0, tidspunktBeregnet = null)
        coEvery { ctx.sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar) } returns
            PensjonsgivendeInntektAarResponse(
                inntektsaar = inntektsaar,
                norskPersonidentifikator = sak.ident,
                pensjonsgivendeInntekt = emptyList(),
            )
        every { ctx.etteroppgjoerOppgaveService.opprettVurderKonsekvensOppgaveForFerdigstiltEtteroppgjoer(any(), any()) } just Runs

        ctx.service.haandterSkatteoppgjoerMottatt(etteroppgjoer, sak)

        verify(exactly = 1) {
            ctx.etteroppgjoerOppgaveService.opprettVurderKonsekvensOppgaveForFerdigstiltEtteroppgjoer(sakId, inntektsaar)
        }
    }

    @Test
    fun `haandterSkatteoppgjoerMottatt oppretter ikke oppgave naar inntekt er uendret siden ferdigstilt etteroppgjoer`() {
        val ctx = TestContext(sakId)
        val inntektsaar = 2024
        val forbehandlingId = UUID.randomUUID()
        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.FERDIGSTILT,
                sisteFerdigstilteForbehandling = forbehandlingId,
            )
        val sak = sak(sakId = sakId)

        every { ctx.forbehandlingDao.hentPensjonsgivendeInntekt(forbehandlingId) } returns
            SummertePensjonsgivendeInntekter(loensinntekt = 0, naeringsinntekt = 0, tidspunktBeregnet = null)
        coEvery { ctx.sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar) } returns
            PensjonsgivendeInntektAarResponse(
                inntektsaar = inntektsaar,
                norskPersonidentifikator = sak.ident,
                pensjonsgivendeInntekt = emptyList(),
            )

        ctx.service.haandterSkatteoppgjoerMottatt(etteroppgjoer, sak)

        verify(exactly = 0) {
            ctx.etteroppgjoerOppgaveService.opprettVurderKonsekvensOppgaveForFerdigstiltEtteroppgjoer(any(), any())
        }
    }

    @Test
    fun `haandterSkatteoppgjoerMottatt returnerer hvis etteroppgjoer har status VENTER_PAA_SVAR`() {
        val ctx = TestContext(sakId)
        val inntektsaar = 2024
        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = EtteroppgjoerStatus.VENTER_PAA_SVAR,
            )
        val sak = sak(sakId = sakId)

        every { ctx.etteroppgjoerOppgaveService.opprettVurderKonsekvensOppgaveForFerdigstiltEtteroppgjoer(any(), any()) } just Runs

        ctx.service.haandterSkatteoppgjoerMottatt(etteroppgjoer, sak)

        verify(exactly = 0) { ctx.dao.oppdaterEtteroppgjoerStatus(any(), any(), any()) }
        verify(exactly = 0) { ctx.etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(any(), any(), any()) }
    }

    @Test
    fun `finnOgOpprettManglendeEtteroppgjoer oppretter etteroppgjoer for manglende aar`() {
        val ctx = TestContext(sakId)
        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        // Innvilget periode f.eks 2024, 2025 og 2026
        coEvery { ctx.vedtakKlient.hentInnvilgedePerioder(any(), any()) } returns
            listOf(
                InnvilgetPeriodeDto(VedtakPeriode(YearMonth.now().minusYears(1), null), mockk(relaxed = true)),
                InnvilgetPeriodeDto(VedtakPeriode(YearMonth.now().minusYears(2), null), mockk(relaxed = true)),
            )

        every { ctx.dao.hentEtteroppgjoerForInntektsaar(sakId, any()) } returns null

        // Ingen eksisterende etteroppgjør
        every { ctx.dao.hentEtteroppgjoerForSak(sakId) } returns
            listOf(
                Etteroppgjoer(
                    sakId = sakId,
                    inntektsaar = Year.now().value - 2, // f.eks 2024
                    status = EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                ),
            )

        runBlocking {
            ctx.service.kunDevFinnOgOpprettManglendeEtteroppgjoer(sakId, brukerTokenInfo)
        }

        // Skal opprette etteroppgjør for 2025
        coVerify(exactly = 1) { ctx.dao.lagreEtteroppgjoer(match { it.inntektsaar == Year.now().value - 1 }) } // f.eks 2025
        coVerify(exactly = 0) { ctx.dao.lagreEtteroppgjoer(match { it.inntektsaar == Year.now().value - 2 }) } // f.eks 2024
        coVerify(exactly = 0) { ctx.dao.lagreEtteroppgjoer(match { it.inntektsaar == Year.now().value }) } // f.eks 2026
    }
}
