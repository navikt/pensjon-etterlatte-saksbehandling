package no.nav.etterlatte.brev.hentinformasjon.behandling

import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentSak(sakId, brukerTokenInfo)

    suspend fun hentVedtaksbehandlingKanRedigeres(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)

    suspend fun hentBrevutfall(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentBrevutfall(behandlingId, brukerTokenInfo)

    suspend fun hentEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentEtterbetaling(behandlingId, brukerTokenInfo)

    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

    suspend fun hentKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentKlage(klageId, brukerTokenInfo)

    suspend fun hentSisteIverksatteBehandling(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo)

    suspend fun hentKlageForBehandling(
        behandlingId: UUID,
        sakId: Long,
        bruker: BrukerTokenInfo,
    ): Klage? {
        val hentKlagerForSak = behandlingKlient.hentKlagerForSak(sakId, bruker)
        return hentKlagerForSak.firstOrNull {
            it.formkrav
                ?.formkrav
                ?.vedtaketKlagenGjelder
                ?.behandlingId
                ?.let { UUID.fromString(it) } == behandlingId
        }
    }
}
