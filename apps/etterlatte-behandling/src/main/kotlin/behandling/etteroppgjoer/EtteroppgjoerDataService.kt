package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.SisteAvkortingOgOpphoer
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerDataService(
    val behandlingService: BehandlingService,
    val featureToggleService: FeatureToggleService,
    val vedtakKlient: VedtakKlient,
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
}
