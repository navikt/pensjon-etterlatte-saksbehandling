package no.nav.etterlatte.brev

import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.vedtaksbehandling.VedtaksbehandlingService
import no.nav.etterlatte.behandling.vedtaksbehandling.VedtaksbehandlingType
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class BrevService(
    val vedtaksbehandlingService: VedtaksbehandlingService,
    val nyBrevKlient: BrevKlient,
    val brevKlient: BrevApiKlient, // Gammel løsning (brev-api bygger brevdata)
    val vedtakKlient: VedtakKlient,
    val tilbakekrevingBrevService: TilbakekrevingBrevService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        if (bruker is Saksbehandler) {
            val kanRedigeres =
                inTransaction {
                    vedtaksbehandlingService.erBehandlingRedigerbar(behandlingId)
                }
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        val vedtaksbehandlingType =
            inTransaction {
                vedtaksbehandlingService.hentVedtaksbehandling(behandlingId).type
            }
        when (vedtaksbehandlingType) {
            VedtaksbehandlingType.TILBAKEKREVING ->
                tilbakekrevingBrevService.opprettVedtaksbrev(behandlingId, sakId, bruker)
            else ->
                brevKlient.opprettVedtaksbrev(behandlingId, sakId, bruker)
        }
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val vedtak =
            vedtakKlient.hentVedtak(behandlingId, bruker)
                ?: throw InternfeilException("Mangler vedtak for behandling (id=$behandlingId)")
        val saksbehandlerident: String = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: bruker.ident()

        val skalLagrePdf =
            if (vedtak.status != VedtakStatus.FATTET_VEDTAK) {
                logger.info("Vedtak status er ${vedtak.status}. Avventer ferdigstilling av brev (behandlingId=$behandlingId)")
                false
            } else if (bruker.erSammePerson(saksbehandlerident)) {
                logger.warn(
                    "Kan ikke ferdigstille/låse brev når saksbehandler ($saksbehandlerident)" +
                        " og attestant (${bruker.ident()}) er samme person.",
                )
                false
            } else {
                true
            }

        val vedtaksbehandlingType =
            inTransaction {
                vedtaksbehandlingService.hentVedtaksbehandling(behandlingId).type
            }
        return when (vedtaksbehandlingType) {
            VedtaksbehandlingType.TILBAKEKREVING ->
                tilbakekrevingBrevService.genererPdf(brevID, behandlingId, sakId, bruker, skalLagrePdf)
            else ->
                brevKlient.genererPdf(brevID, behandlingId, bruker)
        }
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

        nyBrevKlient.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
    }

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        if (bruker is Saksbehandler) {
            val kanRedigeres =
                inTransaction {
                    vedtaksbehandlingService.erBehandlingRedigerbar(behandlingId)
                }
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        val vedtaksbehandlingType = vedtaksbehandlingService.hentVedtaksbehandling(behandlingId).type
        return when (vedtaksbehandlingType) {
            VedtaksbehandlingType.TILBAKEKREVING ->
                tilbakekrevingBrevService.tilbakestillVedtaksbrev(brevID, behandlingId, sakId, bruker)
            else ->
                brevKlient.tilbakestillVedtaksbrev(brevID, behandlingId, bruker)
        }
    }
}

class KanIkkeOppretteVedtaksbrev(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "KAN_IKKE_ENDRE_VEDTAKSBREV",
        detail = "Statusen til behandlingen tillater ikke at det opprettes vedtaksbrev",
        meta = mapOf("behandlingId" to behandlingId),
    )

class SaksbehandlerOgAttestantSammePerson(
    saksbehandler: String,
    attestant: String,
) : UgyldigForespoerselException(
        code = "SAKSBEHANDLER_OG_ATTESTANT_SAMME_PERSON",
        detail = "Kan ikke ferdigstille vedtaksbrevet når saksbehandler ($saksbehandler) og attestant ($attestant) er samme person.",
        meta = mapOf("saksbehandler" to saksbehandler, "attestant" to attestant),
    )
