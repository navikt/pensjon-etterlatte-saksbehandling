package journalpost

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import org.slf4j.MDC
import java.util.*

class JournalpostClient(
    private val httpClient: HttpClient,
    private val restApiUrl: String,
    private val graphqlApiUrl: String
) : JournalpostService {

    val configLocation: String? = null
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()
    val azureAdClient = AzureAdClient(config, httpClient)

    override suspend fun hentInnkommendeBrevInnhold(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray = try {
        httpClient.get("$restApiUrl/$journalpostId/$dokumentInfoId/ARKIV") {
            header("Authorization", "Bearer ${getToken(accessToken)}")
            header("Content-Type", "application/json")
            header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
        }.body()
    } catch (ex: Exception) {
        throw JournalpostException("Feil ved kall til hentdokument", ex)
    }

    override suspend fun hentInnkommendeBrev(
        fnr: String,
        idType: BrukerIdType,
        accessToken: String
    ): JournalpostResponse {

        val request = GraphqlRequest(
            query = getQuery("/graphql/journalpost.graphql"),
            variables = dokumentOversiktBrukerVariables(
                brukerId = BrukerId(
                    id = fnr,
                    type = idType
                ),
                10 // TODO: Finn en grense eller fiks paginering
            )
        )

        return retry<JournalpostResponse> {
            httpClient.post(graphqlApiUrl) {
                header("Authorization", "Bearer ${getToken(accessToken)}")
                accept(ContentType.Application.Json)
                setBody(TextContent(request.toJson(), ContentType.Application.Json))
            }.body()
        }.let {
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

    private suspend fun getToken(accessToken: String): String {
        val token = azureAdClient.getOnBehalfOfAccessTokenForResource(listOf("api://dev-fss.teamdokumenthandtering.saf/.default"), accessToken)
        return token.get()?.accessToken ?: ""
    }
}

class JournalpostException(msg: String, cause: Throwable) : Exception(msg, cause)

data class JournalpostResponse(
    val data: DokumentoversiktBruker? = null,
    val errors: List<JournalpostResponseError>? = null
)

data class DokumentoversiktBruker(
    val dokumentoversiktBruker: Journalposter
)

data class Journalposter(
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

data class dokumentOversiktBrukerVariables(
    val brukerId: BrukerId,
    val foerste: Int
)

data class BrukerId(
    val id: String,
    val type: BrukerIdType,
)
