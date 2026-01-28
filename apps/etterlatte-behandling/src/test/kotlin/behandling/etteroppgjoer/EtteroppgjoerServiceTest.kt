package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
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
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerServiceTest {
    private val sakId = sakId1

    private class TestContext(
        val sakId: SakId,
    ) {
        val dao: EtteroppgjoerDao = mockk()
        val sigrunKlient: SigrunKlient = mockk()
        val beregningKlient: BeregningKlient = mockk()
        val behandlingService: BehandlingService = mockk()
        val vedtakKlient: VedtakKlient = mockk()

        val service =
            EtteroppgjoerService(
                dao = dao,
                vedtakKlient = vedtakKlient,
                behandlingService = behandlingService,
                beregningKlient = beregningKlient,
                sigrunKlient = sigrunKlient,
            )

        val sisteIverksatteBehandlingId = UUID.randomUUID()

        init {
            every { dao.hentEtteroppgjoerForInntektsaar(sakId, any()) } returns
                mockk {
                    every { status } returns EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
                }
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
                    inntektsaar = ETTEROPPGJOER_AAR,
                    status = status,
                ),
            )

        resultat.shouldNotBeNull()
    }

    @Test
    fun `skal kaste feil hvis ikke aktivt etteroppgjør for sak`() {
        val ctx = TestContext(sakId)
        ctx.ingenEtteroppgjoer()

        assertThrows<InternfeilException> {
            ctx.service.hentAktivtEtteroppgjoerForSak(sakId)
        }

        ctx.returnerEtteroppgjoer(
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = ETTEROPPGJOER_AAR,
                status = EtteroppgjoerStatus.FERDIGSTILT,
            ),
        )

        assertThrows<InternfeilException> {
            ctx.service.hentAktivtEtteroppgjoerForSak(sakId)
        }
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
                ctx.service.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                    sistIverksatteBehandling = foerstegangsBehandling,
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
                cx.service.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
                    sistIverksatteBehandling = foerstegangsBehandling,
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
                mottattSkatteoppgjoer = true,
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
                mottattSkatteoppgjoer = true,
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
                    mottattSkatteoppgjoer = true,
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
                mottattSkatteoppgjoer = true,
            )

        assertThrows<IkkeTillattException> {
            ctx.service.oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandling)
        }
    }

    @Test
    fun `opprettEtteroppgjoer returnerer null dersom etteroppgjør allerede finnes for år`() {
        val ctx = TestContext(sakId)
        every { ctx.dao.hentEtteroppgjoerForInntektsaar(sakId, 2024) } returns
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = 2024,
                status = EtteroppgjoerStatus.FERDIGSTILT,
            )

        val opprettetEtteroppgjoer = runBlocking { ctx.service.upsertNyttEtteroppgjoer(sakId, 2024) }
        assertNull(opprettetEtteroppgjoer)
        coVerify(exactly = 0) { ctx.vedtakKlient.hentIverksatteVedtak(any(), any()) }
        coVerify(exactly = 0) { ctx.dao.lagreEtteroppgjoer(any()) }
    }

    @Test
    fun `opprettEtteroppgjoer returnerer nytt etteroppgjør dersom etteroppgjør ikke finnes for år`() {
        val ctx = TestContext(sakId)
        every { ctx.dao.hentEtteroppgjoerForInntektsaar(sakId, 2024) } returns
            null

        val resultat = runBlocking { ctx.service.upsertNyttEtteroppgjoer(sakId, 2024) }

        with(resultat!!) {
            this.sakId shouldBe ctx.sakId
            this.inntektsaar shouldBe 2024
            this.status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
        }
        coVerify(exactly = 1) { ctx.dao.lagreEtteroppgjoer(any()) }
    }

    @Test
    fun `opprettEtteroppgjoer oppdaterer etteroppgjør dersom behandling ikke er påbegynt`() {
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

        val resultat = runBlocking { ctx.service.upsertNyttEtteroppgjoer(sakId, 2024) }

        with(resultat!!) {
            this.sakId shouldBe ctx.sakId
            this.inntektsaar shouldBe 2024
            this.status shouldBe EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
            this.harUtlandstilsnitt shouldBe true
        }
        coVerify(exactly = 1) { ctx.dao.lagreEtteroppgjoer(any()) }
    }

    private fun utlandstilsnitt(): Utlandstilknytning =
        Utlandstilknytning(UtlandstilknytningType.UTLANDSTILSNITT, kildeSaksbehandler(), "begrunnelse")

    fun kildeSaksbehandler() = Grunnlagsopplysning.Saksbehandler(ident = "ident", tidspunkt = Tidspunkt(instant = Instant.now()))
}
