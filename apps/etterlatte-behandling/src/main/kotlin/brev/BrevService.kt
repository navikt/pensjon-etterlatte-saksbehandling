package no.nav.etterlatte.brev

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerBrevService
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevType
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
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

            else ->
                videresendInterneFeil {
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

            else ->
                videresendInterneFeil {
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

            else ->
                videresendInterneFeil {
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
                    behandlingId = behandlingId,
                    brukerTokenInfo = bruker,
                )

            else ->
                videresendInterneFeil {
                    brevApiKlient.tilbakestillVedtaksbrev(brevID, behandlingId, sakId, brevType, bruker)
                }
        }
    }

    /**
     * Videresender interne feil fra endepunkt som kalles -- hvis vi ser at feilen kan leses som en exceptionResponse.
     * Burde brukes med omhu, siden det at en app responderer med 404 f.eks. på at noe ikke fins kan fremdeles
     * være en internfeil i appen som kaller det.
     *
     * Men for de kallene som blir "proxyet" rett til brev-api er det bedre å få med riktig feilmelding ut til
     * saksbehandler, siden logikken og feilmeldingene kommer fra brev-api.
     */
    private suspend fun <T> videresendInterneFeil(eksterntKall: suspend () -> T): T {
        try {
            return eksterntKall()
        } catch (responseException: ResponseException) {
            val exceptionResponse =
                try {
                    responseException.response.body<ExceptionResponse>()
                } catch (internException: Exception) {
                    logger.info(
                        "Kunne ikke parse ut feil som en ExceptionResponse, så vi fikk ikke feilmelding i body. " +
                            "Kaster opprinnelig exception videre",
                        internException,
                    )
                    throw internException
                }
            when (exceptionResponse.status) {
                // ForespoerselException
                in 400..499 -> {
                    val videresendtForespoerselException =
                        ForespoerselException(
                            status = exceptionResponse.status,
                            code = exceptionResponse.code ?: "UKJENT_FEIL",
                            detail = exceptionResponse.detail,
                            cause = responseException,
                        )
                    logger.warn(
                        "Mottok forespørselexception, som propageres videre",
                        videresendtForespoerselException,
                    )
                    throw videresendtForespoerselException
                }

                // InternfeilException
                in 500..599 -> {
                    val videresendtInternfeilException =
                        InternfeilException(
                            detail = exceptionResponse.detail,
                            cause = responseException,
                        )
                    logger.warn("Mottok internfeil, som propageres videre", videresendtInternfeilException)
                    throw videresendtInternfeilException
                }

                // Ukjent feilmelding, bare kast original feilmelding videre
                else -> throw responseException
            }
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
                videresendInterneFeil {
                    brevApiKlient.hentVedtaksbrev(behandlingId, bruker)
                }
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
