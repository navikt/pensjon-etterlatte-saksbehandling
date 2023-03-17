package no.nav.etterlatte.brev.dokument

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import no.nav.etterlatte.brev.journalpost.BrukerIdType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import org.slf4j.MDC
import java.util.UUID

/*
* SAF (Sak- og Arkiv Facade)
*/
class SafClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val safScope: String
) : SafService {

    private val configLocation: String? = null
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()
    private val azureAdClient = AzureAdClient(config, httpClient)

    override suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray = try {
        httpClient.get("$baseUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
            header("Authorization", "Bearer ${getToken(accessToken)}")
            header("Content-Type", "application/json")
            header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
        }.body()
    } catch (ex: Exception) {
        throw JournalpostException("Feil ved kall til hentdokument", ex)
    }

    override suspend fun hentDokumenter(
        fnr: String,
        idType: BrukerIdType,
        accessToken: String
    ): JournalpostResponse {
        val request = GraphqlRequest(
            query = getQuery("/graphql/journalpost.graphql"),
            variables = DokumentOversiktBrukerVariables(
                brukerId = BrukerId(
                    id = fnr,
                    type = idType
                ),
                10 // TODO: Finn en grense eller fiks paginering
            )
        )

        return retry<JournalpostResponse> {
            httpClient.post("$baseUrl/graphql") {
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
        val token = azureAdClient.getOnBehalfOfAccessTokenForResource(
            listOf(safScope),
            accessToken
        )
        return token.get()?.accessToken ?: ""
    }
}

class JournalpostException(msg: String, cause: Throwable) : Exception(msg, cause)