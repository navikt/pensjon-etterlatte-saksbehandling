package no.nav.etterlatte.brev

import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BrevService(
    // val vedtaksbehandlingService: VedtaksbehandlingService, TODO
    val brevKlient: BrevKlient,
    val vedtakKlient: VedtakKlient,
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

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val vedtakDto =
            krevIkkeNull(vedtakKlient.hentVedtak(behandlingId, brukerTokenInfo)) {
                "Fant ikke vedtak for behandling (id=$behandlingId)"
            }
        val saksbehandlerIdent = vedtakDto.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident()

        if (vedtakDto.status != VedtakStatus.FATTET_VEDTAK) {
            throw IllegalStateException(
                "Vedtak status er ${vedtakDto.status}. Avventer ferdigstilling av brev (behandlingId=$behandlingId)",
            )
        }
        if (brukerTokenInfo.erSammePerson(saksbehandlerIdent)) {
            throw SaksbehandlerOgAttestantSammePerson(saksbehandlerIdent, brukerTokenInfo.ident())
        }

        brevKlient.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
    }
}

class SaksbehandlerOgAttestantSammePerson(
    saksbehandler: String,
    attestant: String,
) : UgyldigForespoerselException(
        code = "SAKSBEHANDLER_OG_ATTESTANT_SAMME_PERSON",
        detail = "Kan ikke ferdigstille vedtaksbrevet når saksbehandler ($saksbehandler) og attestant ($attestant) er samme person.",
        meta = mapOf("saksbehandler" to saksbehandler, "attestant" to attestant),
    )
