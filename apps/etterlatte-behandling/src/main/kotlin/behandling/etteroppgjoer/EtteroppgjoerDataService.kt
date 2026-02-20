package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.logger

class EtteroppgjoerDataService(
    val behandlingService: BehandlingService,
    val featureToggleService: FeatureToggleService,
    val vedtakKlient: VedtakKlient,
    val beregningKlient: BeregningKlient,
) {
    fun sisteVedtakMedAvkorting(
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): VedtakSammendragDto =
        sisteVedtakMedAvkorting(
            hentIverksatteVedtak(sakId, bruker),
        )

    fun sisteVedtakMedAvkorting(vedtakListe: List<VedtakSammendragDto>): VedtakSammendragDto =
        vedtakListe
            .sortedByDescending { it.datoAttestert }
            .first { it.vedtakType != VedtakType.OPPHOER }

    fun vedtakMedGjeldendeOpphoer(vedtakListe: List<VedtakSammendragDto>): VedtakSammendragDto? {
        val sisteVedtak =
            vedtakListe
                .sortedByDescending { it.datoAttestert }
                .first()

        if (sisteVedtak.vedtakType == VedtakType.OPPHOER) {
            return sisteVedtak
        } else {
            val sisteVedtakMedAvkorting = sisteVedtakMedAvkorting(vedtakListe)
            if (sisteVedtakMedAvkorting.opphoerFraOgMed != null) {
                return sisteVedtakMedAvkorting
            }
        }
        return null
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
            logger.warn("Kunne ikke hente tidligere avkorting for behandling med id=${sisteIverksatteBehandling.id}", e)
            null
        }
    }

    fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> =
        runBlocking {
            vedtakKlient
                .hentIverksatteVedtak(sakId, brukerTokenInfo)
        }
}
