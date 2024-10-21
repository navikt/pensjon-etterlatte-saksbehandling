package no.nav.etterlatte.brev.hentinformasjon.behandling

import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentSak(
        sakId: SakId,
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
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo)

    suspend fun hentTidligereFamiliepleier(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentTidligereFamiliepleier(behandlingId, brukerTokenInfo)
}
