package no.nav.etterlatte.gyldigsoeknad.journalfoering

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.journalpost.dokarkiv.OpprettJournalpostRequest
import no.nav.etterlatte.libs.journalpost.dokarkiv.OpprettJournalpostResponse
import org.slf4j.LoggerFactory

class DokarkivKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(DokarkivKlient::class.java)

    internal suspend fun opprettJournalpost(
        request: OpprettJournalpostRequest,
        forsoekFerdigstill: Boolean = true,
    ): OpprettJournalpostResponse {
        val response =
            client.post("$url?forsoekFerdigstill=$forsoekFerdigstill") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return if (response.status.isSuccess()) {
            response
                .body<OpprettJournalpostResponse>()
                .also {
                    logger.info(
                        "Journalpost opprettet (journalpostId=${it.journalpostId}, ferdigstilt=${it.journalpostferdigstilt})",
                    )
                }
        } else if (response.status == HttpStatusCode.Conflict) {
            response
                .body<OpprettJournalpostResponse>()
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
}
