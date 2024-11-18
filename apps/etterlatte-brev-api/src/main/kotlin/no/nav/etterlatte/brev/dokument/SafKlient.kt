package no.nav.etterlatte.brev.dokument

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.get
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdHttpClient
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

/*
* SAF (Sak- og Arkiv Facade)
*/
class SafKlient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val safScope: String,
) {
    private val logger = LoggerFactory.getLogger(SafService::class.java)

    private val configLocation: String? = null
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()
    private val azureAdClient = AzureAdClient(config, AzureAdHttpClient(httpClient))

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        bruker: BrukerTokenInfo,
    ): ByteArray =
        try {
            httpClient
                .get("$baseUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
                    bearerAuth(getOboToken(bruker))
                    contentType(ContentType.Application.Json)
                }.body()
        } catch (re: ResponseException) {
            when (re.response.status) {
                HttpStatusCode.Forbidden -> {
                    val errorMessage = re.response.body<JsonNode>()["message"]?.asText()
                    // TODO bedre håndtering av dette? https://jira.adeo.no/browse/EY-4755
                    logger.warn(errorMessage ?: "Feil fra Saf: ${re.response.bodyAsText()}")

                    throw IkkeTilgangTilJournalpost()
                }
                HttpStatusCode.NotFound -> {
                    throw IkkeFunnetException(
                        code = "JOURNALPOST_DOKUMENT_IKKE_FUNNET",
                        detail = "Fant ikke dokumentet i Joark",
                        meta =
                            mapOf(
                                "journalpostId" to journalpostId,
                                "dokumentInfoId" to dokumentInfoId,
                                "variantFormat" to "ARKIV",
                            ),
                    )
                }
                else -> {
                    logger.error("Ukjent feil i kall mot Saf: ${re.response.bodyAsText()}")

                    throw ForespoerselException(
                        status = re.response.status.value,
                        code = "UKJENT_FEIL_HENT_JOURNALPOST_PDF",
                        detail = "Ukjent feil oppsto ved henting av journalpost",
                    )
                }
            }
        }

    suspend fun hentDokumenter(
        requestVariables: DokumentOversiktBrukerVariables,
        bruker: BrukerTokenInfo,
    ): DokumentoversiktBrukerResponse {
        val request =
            GraphqlRequest(
                query = getQuery("/graphql/dokumentoversiktBruker.graphql"),
                variables = requestVariables,
            )

        return retry {
            val res =
                httpClient.post("$baseUrl/graphql") {
                    bearerAuth(getOboToken(bruker))
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (res.status.isSuccess()) {
                res.body<DokumentoversiktBrukerResponse>()
            } else {
                throw ResponseException(res, "Ukjent feil oppsto ved henting av journalposter")
            }
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentJournalpost(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): JournalpostResponse {
        val request =
            GraphqlRequest(
                query = getQuery("/graphql/journalpost.graphql"),
                variables = JournalpostVariables(journalpostId),
            )

        return retry {
            val res =
                httpClient.post("$baseUrl/graphql") {
                    bearerAuth(getOboToken(bruker))
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (res.status.isSuccess()) {
                res.body<JournalpostResponse>()
            } else {
                throw ResponseException(res, "Ukjent feil oppsto ved henting av journalposter")
            }
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentUtsendingsInfo(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): HentUtsendingsinfoResponse {
        val request =
            GraphqlRequest(
                query = getQuery("/graphql/utsendingsinfo.graphql"),
                variables = JournalpostVariables(journalpostId),
            )

        return retry {
            val res =
                httpClient.post("$baseUrl/graphql") {
                    bearerAuth(getOboToken(bruker))
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (res.status.isSuccess()) {
                res.body<HentUtsendingsinfoResponse>()
            } else {
                throw ResponseException(res, "Ukjent feil oppsto ved henting av journalposter")
            }
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    private fun getQuery(name: String): String =
        javaClass
            .getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    private suspend fun getOboToken(bruker: BrukerTokenInfo): String {
        val token =
            azureAdClient.hentTokenFraAD(
                bruker,
                listOf(safScope),
            )
        return token.get()?.accessToken ?: ""
    }
}
