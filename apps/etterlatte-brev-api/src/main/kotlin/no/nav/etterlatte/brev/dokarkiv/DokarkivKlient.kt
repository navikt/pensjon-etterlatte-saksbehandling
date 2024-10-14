package no.nav.etterlatte.brev.dokarkiv

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class DokarkivKlient(
    config: Config,
) {
    private val logger = LoggerFactory.getLogger(DokarkivKlient::class.java)

    private val downstreamResourceClient = DownstreamResourceClient(AzureAdClient(config))

    private val url = config.getString("dokarkiv.resource.url")
    private val clientId = config.getString("dokarkiv.client.id")

    internal suspend fun opprettJournalpost(
        request: OpprettJournalpost,
        ferdigstill: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettJournalpostResponse =
        downstreamResourceClient
            .post(
                Resource(clientId, "$url?forsoekFerdigstill=$ferdigstill"),
                brukerTokenInfo,
                request,
            ).mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = {
                    if (it is ResponseException) {
                        if (it.response.status == HttpStatusCode.Conflict) {
                            logger.warn("Konflikt på journalpost med eksternReferanseId: ${request.eksternReferanseId}")
                            it.response.body<OpprettJournalpostResponse>()
                        } else {
                            val error = it.response.body<DokarkivErrorResponse>()

                            logger.error("Feil oppsto på opprett journalpost: $error")

                            throw ForespoerselException(
                                status = it.response.status.value,
                                code = "UKJENT_FEIL_VED_JOURNALFOERING",
                                detail = error.message ?: "Ukjent feil oppsto ved journalføring av brev",
                            )
                        }
                    } else {
                        throw it
                    }
                },
            )

    internal suspend fun ferdigstillJournalpost(
        journalpostId: String,
        journalfoerendeEnhet: Enhetsnummer,
        bruker: BrukerTokenInfo,
    ): Boolean =
        downstreamResourceClient
            .patch(
                Resource(clientId, "$url/$journalpostId/ferdigstill"),
                bruker,
                FerdigstillJournalpostRequest(journalfoerendeEnhet),
            ).mapBoth(
                success = {
                    logger.info("Journalpost $journalpostId ferdigstilt med respons: ${it.response}")
                    true
                },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Ukjent feil ved ferdigstillings av journalpostId=$journalpostId: $error")
                        throw KunneIkkeFerdigstilleJournalpost(journalpostId, error.message)
                    } else {
                        throw it
                    }
                },
            )

    internal suspend fun oppdaterJournalpost(
        journalpostId: String,
        request: OppdaterJournalpostRequest,
        bruker: BrukerTokenInfo,
    ): OppdaterJournalpostResponse =
        downstreamResourceClient
            .put(Resource(clientId, "$url/$journalpostId"), bruker, request)
            .mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Feil oppsto på oppdater journalpost: $error")

                        throw ForespoerselException(
                            status = it.response.status.value,
                            code = "FEIL_VED_OPPDATERING_AV_JOURNALPOST",
                            detail = error.message ?: "En ukjent feil oppsto ved oppdatering av journalpost",
                        )
                    } else {
                        throw it
                    }
                },
            )

    internal suspend fun feilregistrerSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): String =
        downstreamResourceClient
            .patch(
                Resource(
                    clientId = clientId,
                    url = "$url/$journalpostId/feilregistrer/feilregistrerSakstilknytning",
                    additionalHeaders =
                        mapOf(
                            HttpHeaders.Accept to ContentType.Text.Plain.toString(),
                        ),
                ),
                bruker,
            ).mapBoth(
                success = { it.response!!.toString() },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Feil oppsto på feilregistrer journalpost: $error")

                        throw ForespoerselException(
                            status = it.response.status.value,
                            code = "FEILREGISTRER_JOURNALPOST_ERROR",
                            detail = error.message ?: "En ukjent feil oppsto ved feilregistrering av journalpost",
                        )
                    } else {
                        throw it
                    }
                },
            )

    internal suspend fun opphevFeilregistrertSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): String =
        downstreamResourceClient
            .patch(
                Resource(
                    clientId,
                    "$url/$journalpostId/feilregistrer/opphevFeilregistrertSakstilknytning",
                    mapOf(
                        HttpHeaders.Accept to ContentType.Text.Plain.toString(),
                    ),
                ),
                bruker,
            ).mapBoth(
                success = { it.response!!.toString() },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Feil oppsto på opphev feilregistrert journalpost: $error")

                        throw ForespoerselException(
                            status = it.response.status.value,
                            code = "OPPHEV_FEILREGISTRERT_SAKSTILKNYTNING_ERROR",
                            detail = error.message ?: "En ukjent feil oppsto ved oppheving av feilregistrert sakstilknytning",
                        )
                    } else {
                        throw it
                    }
                },
            )

    /*
     * Tar en opprettet journalpost ut av ordinær saksbehandling ved å sette status til "AVBRYT"
     */
    internal suspend fun settStatusAvbryt(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): String =
        downstreamResourceClient
            .patch(
                Resource(
                    clientId = clientId,
                    url = "$url/$journalpostId/feilregistrer/settStatusAvbryt",
                    additionalHeaders =
                        mapOf(
                            HttpHeaders.Accept to ContentType.Text.Plain.toString(),
                        ),
                ),
                bruker,
            ).mapBoth(
                success = { it.response!!.toString() },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Feil oppsto ved settStatusAvbryt på journalpost: $error")

                        throw ForespoerselException(
                            status = it.response.status.value,
                            code = "SETT_STATUS_AVBRYT_JOURNALPOST_ERROR",
                            detail = error.message ?: "En ukjent feil oppsto ved sett status avbryt på journalpost",
                        )
                    } else {
                        throw it
                    }
                },
            )

    internal suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
        bruker: BrukerTokenInfo,
    ): KnyttTilAnnenSakResponse =
        downstreamResourceClient
            .put(
                Resource(
                    clientId,
                    "$url/$journalpostId/knyttTilAnnenSak",
                ),
                bruker,
                request,
            ).mapBoth(
                success = { deserialize(it.response!!.toString()) },
                failure = {
                    if (it is ResponseException) {
                        val error = it.response.body<DokarkivErrorResponse>()

                        logger.error("Feil oppsto på knyttTilAnnenSak: $error")

                        throw ForespoerselException(
                            status = it.response.status.value,
                            code = "KNYTT_TIL_ANNEN_SAK_ERROR",
                            detail = error.message ?: "En ukjent feil har oppstått. Kunne ikke knytte journalpost til annen sak",
                        )
                    } else {
                        throw it
                    }
                },
            )
}

data class FerdigstillJournalpostRequest(
    val journalfoerendeEnhet: Enhetsnummer,
)

class KunneIkkeFerdigstilleJournalpost(
    journalpostId: String,
    melding: String? = null,
) : UgyldigForespoerselException(
        code = "KUNNE_IKKE_FERDIGSTILLE_JOURNALPOST",
        detail = melding ?: "Kunne ikke ferdigstille journalpost med id=$journalpostId",
        meta = mapOf("journalpostId" to journalpostId),
    )
