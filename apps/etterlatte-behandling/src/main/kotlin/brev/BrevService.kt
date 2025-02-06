package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BrevService(
    // val vedtaksbehandlingService: VedtaksbehandlingService, TODO
    val tilbakekrevingBrevService: TilbakekrevingBrevService,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ) {
        // vedtaksbehandlingService.erBehandlingRedigerbar() // TODO..
        // TODO finn ut hva slags behandling

        tilbakekrevingBrevService.opprettVedtaksbrev(behandlingId, sakId, bruker)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Pdf {
        // vedtaksbehandlingService.erBehandlingRedigerbar() // TODO..
        // TODO finn ut hva slags behandling
        return tilbakekrevingBrevService.genererPdf(brevID, behandlingId, sakId, bruker)
    }
}
