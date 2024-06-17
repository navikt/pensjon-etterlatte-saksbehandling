package no.nav.etterlatte.joarkhendelser.joark

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory

class SafKlient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(SafKlient::class.java)

    suspend fun hentJournalpost(id: Long): JournalpostResponse {
        logger.info("Forsøker å hente journalpost med id=$id")

        val request = opprettHentJournalpostRequest(id)

        val res =
            httpClient.post("$baseUrl/graphql") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        if (res.status.isSuccess()) {
            try {
                return res.body<JournalpostResponse>()
            } catch (e: Exception) {
                throw ResponseException(res, "Ukjent feil oppsto ved deserialisering av journalpost. id: $id")
            }
        } else {
            throw ResponseException(res, "Ukjent feil oppsto ved henting av journalpost. id: $id")
        }
    }

    private fun opprettHentJournalpostRequest(journalpostId: Long): GraphqlRequest {
        val query =
            javaClass
                .getResource("/graphql/journalpost.id.graphql")!!
                .readText()
                .replace(Regex("[\n\t]"), "")

        return GraphqlRequest(query, JournalpostVariables(journalpostId.toString()))
    }
}
