package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.SisteAvkortingOgOpphoer
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.logger
import java.time.YearMonth

class EtteroppgjoerDataService(
    val behandlingService: BehandlingService,
    val featureToggleService: FeatureToggleService,
    val vedtakKlient: VedtakKlient,
    val beregningKlient: BeregningKlient,
) {
    fun hentSisteIverksatteBehandlingOgOpphoer(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<Behandling, YearMonth?> {
        val sisteAvkortingOgOpphoer = hentSisteIverksatteVedtakIkkeOpphoer(sakId, brukerTokenInfo)
        val behandling =
            behandlingService.hentBehandling(sisteAvkortingOgOpphoer.sisteBehandlingMedAvkorting)
                ?: throw InternfeilException("Fant ikke iverksatt behandling $sisteAvkortingOgOpphoer")

        return behandling to sisteAvkortingOgOpphoer.opphoerFom
    }

    fun hentAvkortingForForbehandling(
        forbehandling: EtteroppgjoerForbehandling,
        sisteIverksatteBehandling: Behandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting? {
        val request =
            EtteroppgjoerBeregnetAvkortingRequest(
                forbehandling = forbehandling.id,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                sakId = sisteIverksatteBehandling.sak.id,
            )

        logger.info("Henter avkorting for forbehandling: $request")
        return try {
            runBlocking {
                beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                    request,
                    brukerTokenInfo,
                )
            }
        } catch (e: Exception) {
            logger.warn("Kunne ikke hente tidligere avkorting for behandling med id=$sisteIverksatteBehandling", e)
            null
        }
    }

    suspend fun hentSisteIverksatteBehandlingMedAvkorting(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteAvkortingOgOpphoer {
        // TODO: Med periodisert vilkårsvurdering kan vi være smartere her
        val iverksatteVedtak =
            vedtakKlient
                .hentIverksatteVedtak(sakId, brukerTokenInfo)
                .sortedByDescending { it.datoFattet }

        val sisteVedtakMedAvkorting = iverksatteVedtak.first { it.vedtakType != VedtakType.OPPHOER }
        val opphoer =
            iverksatteVedtak.firstOrNull {
                it.vedtakType == VedtakType.OPPHOER &&
                    it.datoAttestert!! > sisteVedtakMedAvkorting.datoAttestert!!
            }

        return SisteAvkortingOgOpphoer(
            sisteBehandlingMedAvkorting = sisteVedtakMedAvkorting.behandlingId,
            opphoerFom = opphoer?.virkningstidspunkt ?: sisteVedtakMedAvkorting.opphoerFraOgMed,
        )
    }

    private fun hentSisteIverksatteVedtakIkkeOpphoer(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteAvkortingOgOpphoer =
        runBlocking {
            if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_OPPHOER_SKYLDES_DOEDSFALL, false)) {
                hentSisteIverksatteBehandlingMedAvkorting(sakId, brukerTokenInfo)
            } else {
                val iverksatteVedtak =
                    vedtakKlient
                        .hentIverksatteVedtak(sakId, brukerTokenInfo)
                        .sortedByDescending { it.datoFattet }

                val sisteIverksatteVedtak =
                    iverksatteVedtak.firstOrNull()
                        ?: throw InternfeilException("Fant ingen iverksatte vedtak for sak $sakId")

                if (sisteIverksatteVedtak.vedtakType == VedtakType.OPPHOER) {
                    throw InternfeilException("Siste iverksatte vedtak er et opphør, dette er ikke støttet enda")
                }

                if (sisteIverksatteVedtak.opphoerFraOgMed != null) {
                    throw InternfeilException("Siste iverksatte vedtak har opphør fra og med, dette er ikke støttet enda")
                }

                SisteAvkortingOgOpphoer(sisteIverksatteVedtak.behandlingId, null)
            }
        }
}
