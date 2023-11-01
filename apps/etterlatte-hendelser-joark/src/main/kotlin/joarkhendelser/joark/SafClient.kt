package joarkhendelser.joark

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.joarkhendelser.joark.GraphqlRequest
import no.nav.etterlatte.joarkhendelser.joark.HentJournalpostResult
import no.nav.etterlatte.joarkhendelser.joark.JournalpostResponse
import no.nav.etterlatte.joarkhendelser.joark.JournalpostVariables
import org.slf4j.LoggerFactory

class SafClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val safScope: String,
) {
    private val logger = LoggerFactory.getLogger(SafClient::class.java)

    suspend fun hentJournalpost(id: String): HentJournalpostResult {
        logger.info("Forsøker å hente journalpost med id=$id")

        val request = opprettHentJournalpostRequest(id)

        val res =
            httpClient.post("$baseUrl/graphql") {
                accept(ContentType.Application.Json)
                setBody(request)
            }

        return if (res.status.isSuccess()) {
            val journalpost = res.body<JournalpostResponse>().data?.journalpost

            HentJournalpostResult(journalpost)
        } else {
            throw ResponseException(res, "Ukjent feil oppsto ved henting av journalposter")
        }
    }

    private fun opprettHentJournalpostRequest(journalpostId: String): GraphqlRequest {
        val query =
            javaClass.getResource("/graphql/journalpost.id.graphql")!!
                .readText()
                .replace(Regex("[\n\t]"), "")

        return GraphqlRequest(query, JournalpostVariables(journalpostId))
    }
}
