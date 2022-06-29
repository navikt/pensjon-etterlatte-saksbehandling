package journalpost

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.MDC
import java.util.*

class JournalpostClient(
    private val httpClient: HttpClient,
    private val restApiUrl: String,
    private val graphqlApiUrl: String
): JournalpostService {

    override suspend fun hentInnkommendeBrevInnhold(journalpostId: String, dokumentInfoId: String, accessToken: String): ByteArray = try {
        httpClient.get("$restApiUrl/$journalpostId/$dokumentInfoId/ARKIV") {
            header("Authorization", "Bearer $accessToken")
            header("Content-Type", "application/json")
            header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
        }
    } catch (ex: Exception) {
        throw JournalpostException("Feil ved kall til hentdokument", ex)
    }

    override suspend fun hentInnkommendeBrev(fnr: String, idType: BrukerIdType, accessToken: String): JournalpostResponse {
        val request = GraphqlRequest(
            query = getQuery("/graphql/journalpost.graphql"),
            variables = dokumentOversiktBrukerVariables(
                brukerId = BrukerId(
                    id = fnr,
                    type = idType
                ),
                3 // TODO: Finn en grense
            )
        )

        return retry<JournalpostResponse> {
            httpClient.post(graphqlApiUrl) {
                header("Authorization", "Bearer $accessToken")
                accept(ContentType.Application.Json)
                body = TextContent(request.toJson(), ContentType.Application.Json)
            }
        }.let{
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }


    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }
}

class JournalpostException(msg: String, cause: Throwable) : Exception(msg, cause)

data class JournalpostResponse(
    val data: DokumentoversiktBruker? = null,
    val errors: List<JournalpostResponseError>? = null
)

data class DokumentoversiktBruker (
    val dokumentoversiktBruker: Journalposter
)

data class Journalposter (
    val journalposter: List<Journalpost>
)

data class JournalpostResponseError(
    val message: String?,
    val locations: List<PdlErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: PdlErrorExtension? = null
)

data class PdlErrorLocation(
    val line: String?,
    val column: String?
)

data class PdlErrorExtension(
    val code: String?,
    val details: String?,
    val classification: String?
)

data class GraphqlRequest(
    val query: String,
    val variables: dokumentOversiktBrukerVariables
)

data class dokumentOversiktBrukerVariables (
    val brukerId: BrukerId,
    val foerste: Int
)

data class BrukerId (
    val id: String,
    val type: BrukerIdType,
)
