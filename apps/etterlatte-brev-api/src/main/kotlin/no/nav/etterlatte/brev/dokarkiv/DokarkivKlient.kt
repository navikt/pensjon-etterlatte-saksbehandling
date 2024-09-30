package no.nav.etterlatte.brev.dokarkiv

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import org.slf4j.LoggerFactory

class DokarkivKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(DokarkivKlient::class.java)

    internal suspend fun opprettJournalpost(
        request: OpprettJournalpost,
        ferdigstill: Boolean,
    ): OpprettJournalpostResponsee {
        val response =
            client.post("$url?forsoekFerdigstill=$ferdigstill") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return if (response.status.isSuccess()) {
            response
                .body<OpprettJournalpostResponsee>()
                .also {
                    logger.info(
                        "Journalpost opprettet (journalpostId=${it.journalpostId}, ferdigstilt=${it.journalpostferdigstilt})",
                    )
                }
        } else if (response.status == HttpStatusCode.Conflict) {
            response
                .body<OpprettJournalpostResponsee>()
                .also { logger.warn("Konflikt ved lagring av journalpost ${it.journalpostId}") }
        } else {
            logger.error("Feil oppsto på opprett journalpost: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "UKJENT_FEIL_VED_JOURNALFOERING",
                detail = "Ukjent feil oppsto ved journalføring av brev",
            )
        }
    }

    internal suspend fun ferdigstillJournalpost(
        journalpostId: String,
        journalfoerendeEnhet: Enhetsnummer,
    ): Boolean {
        val response =
            client.patch("$url/$journalpostId/ferdigstill") {
                contentType(ContentType.Application.Json)
                setBody(FerdigstillJournalpostRequest(journalfoerendeEnhet))
            }

        return if (response.status.isSuccess()) {
            logger.info("Journalpost $journalpostId ferdigstilt med respons: ${response.body<String>()}")
            true
        } else {
            val errorResponseJson = response.body<JsonNode>()
            val errorMessage = errorResponseJson["message"]?.asText()

            logger.error("Ukjent feil ved ferdigstillings av journalpostId=$journalpostId: $errorResponseJson")
            throw KunneIkkeFerdigstilleJournalpost(journalpostId, errorMessage)
        }
    }

    internal suspend fun oppdaterJournalpost(
        journalpostId: String,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse {
        val response =
            client.put("$url/$journalpostId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return if (response.status.isSuccess()) {
            response.body()
        } else {
            logger.error("Feil oppsto på oppdater journalpost: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "FEIL_VED_OPPDATERING_AV_JOURNALPOST",
                detail = "En ukjent feil oppsto ved oppdatering av journalpost",
            )
        }
    }

    internal suspend fun feilregistrerSakstilknytning(journalpostId: String): String {
        val response =
            client.patch("$url/$journalpostId/feilregistrer/feilregistrerSakstilknytning") {
                contentType(ContentType.Application.Json)
            }

        return if (response.status.isSuccess()) {
            response.body()
        } else {
            logger.error("Feil oppsto på feilregistrer journalpost: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "FEILREGISTRER_JOURNALPOST_ERROR",
                detail = "En ukjent feil oppsto ved feilregistrering av journalpost",
            )
        }
    }

    internal suspend fun opphevFeilregistrertSakstilknytning(journalpostId: String): String {
        val response =
            client.patch("$url/$journalpostId/feilregistrer/opphevFeilregistrertSakstilknytning") {
                contentType(ContentType.Application.Json)
            }

        return if (response.status.isSuccess()) {
            response.body()
        } else {
            logger.error("Feil oppsto på opphev feilregistrert journalpost: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "OPPHEV_FEILREGISTRERT_SAKSTILKNYTNING_ERROR",
                detail = "En ukjent feil oppsto ved oppheving av feilregistrert sakstilknytning",
            )
        }
    }

    internal suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
    ): KnyttTilAnnenSakResponse {
        val response =
            client.put("$url/$journalpostId/knyttTilAnnenSak") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return if (response.status.isSuccess()) {
            response.body()
        } else {
            logger.error("Feil oppsto på knyttTilAnnenSak: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "KNYTT_TIL_ANNEN_SAK_ERROR",
                detail = "En ukjent feil har oppstått. Kunne ikke knytte journalpost til annen sak",
            )
        }
    }
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
