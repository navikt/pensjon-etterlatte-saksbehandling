package no.nav.etterlatte.brev

import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerBrevService
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevType
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
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
    private val behandlingMedBrevService: BehandlingMedBrevService,
    private val brevApiKlient: BrevApiKlient, // Gammel løsning (brev-api bygger brevdata)
    private val vedtakKlient: VedtakKlient,
    private val tilbakekrevingBrevService: TilbakekrevingBrevService,
    private val etteroppgjoerBrevService: EtteroppgjoerBrevService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettStrukturertBrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        if (bruker is Saksbehandler) {
            val kanRedigeres = behandlingMedBrevService.erBehandlingRedigerbar(behandlingId)
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        val behandlingMedBrevType = behandlingMedBrevService.hentBehandlingMedBrev(behandlingId).type
        return when (behandlingMedBrevType) {
            BehandlingMedBrevType.TILBAKEKREVING ->
                tilbakekrevingBrevService.opprettVedtaksbrev(behandlingId, sakId, bruker)

            BehandlingMedBrevType.ETTEROPPGJOER ->
                etteroppgjoerBrevService.opprettEtteroppgjoerBrev(behandlingId, bruker)

            else -> {
                brevApiKlient.opprettVedtaksbrev(behandlingId, sakId, bruker)
            }
        }
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val behandlingMedBrevType = behandlingMedBrevService.hentBehandlingMedBrev(behandlingId).type

        val skalLagrePdf =
            if (behandlingMedBrevType.harVedtaksbrev) {
                val vedtak =
                    vedtakKlient.hentVedtak(behandlingId, bruker)
                        ?: throw InternfeilException("Mangler vedtak for behandling (id=$behandlingId)")
                val saksbehandlerident: String = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: bruker.ident()

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
            } else {
                // TODO: se på statusen for behandlingstypen EO (og andre) her
                false
            }

        return when (behandlingMedBrevType) {
            BehandlingMedBrevType.TILBAKEKREVING ->
                tilbakekrevingBrevService.genererPdf(brevID, behandlingId, sakId, bruker, skalLagrePdf)

            BehandlingMedBrevType.ETTEROPPGJOER ->
                etteroppgjoerBrevService.genererPdf(brevID, behandlingId, bruker)

            else -> {
                brevApiKlient.genererPdf(brevID, behandlingId, bruker)
            }
        }
    }

    suspend fun ferdigstillStrukturertBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandlingMedBrevType = behandlingMedBrevService.hentBehandlingMedBrev(behandlingId).type
        if (behandlingMedBrevType.harVedtaksbrev) {
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
        }

        when (behandlingMedBrevType) {
            BehandlingMedBrevType.TILBAKEKREVING ->
                tilbakekrevingBrevService.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)

            BehandlingMedBrevType.ETTEROPPGJOER ->
                etteroppgjoerBrevService.ferdigstillBrev(behandlingId, brukerTokenInfo)

            else -> {
                brevApiKlient.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
            }
        }
    }

    suspend fun tilbakestillStrukturertBrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brevType: Brevtype,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        if (bruker is Saksbehandler) {
            val kanRedigeres = behandlingMedBrevService.erBehandlingRedigerbar(behandlingId)
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }
        val behandlingMedBrevType = behandlingMedBrevService.hentBehandlingMedBrev(behandlingId).type
        return when (behandlingMedBrevType) {
            BehandlingMedBrevType.TILBAKEKREVING ->
                tilbakekrevingBrevService.tilbakestillVedtaksbrev(brevID, behandlingId, sakId, bruker)

            BehandlingMedBrevType.ETTEROPPGJOER ->
                etteroppgjoerBrevService.tilbakestillEtteroppgjoerBrev(
                    brevId = brevID,
                    forbehandlingId = behandlingId,
                    brukerTokenInfo = bruker,
                )

            else ->
                brevApiKlient.tilbakestillVedtaksbrev(brevID, behandlingId, sakId, brevType, bruker)
        }
    }

    suspend fun hentStrukturertBrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? {
        val vedtaksbehandlingType = behandlingMedBrevService.hentBehandlingMedBrev(behandlingId).type
        return when (vedtaksbehandlingType) {
            BehandlingMedBrevType.TILBAKEKREVING ->
                tilbakekrevingBrevService.hentVedtaksbrev(behandlingId, bruker)

            BehandlingMedBrevType.ETTEROPPGJOER ->
                etteroppgjoerBrevService.hentEtteroppgjoersbrev(behandlingId, bruker)

            else ->
                brevApiKlient.hentVedtaksbrev(behandlingId, bruker)
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
