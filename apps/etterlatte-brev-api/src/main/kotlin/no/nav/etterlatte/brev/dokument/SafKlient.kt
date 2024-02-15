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
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.token.BrukerTokenInfo
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
    private val azureAdClient = AzureAdClient(config, httpClient)

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        bruker: BrukerTokenInfo,
    ): ByteArray =
        try {
            httpClient.get("$baseUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
                bearerAuth(getOboToken(bruker))
                contentType(ContentType.Application.Json)
            }.body()
        } catch (re: ResponseException) {
            logger.error("Feil i kall mot Saf: ${re.response.bodyAsText()}")

            if (re.response.status == HttpStatusCode.Forbidden) {
                val errorMessage = re.response.body<JsonNode>()["message"]?.asText()
                logger.error(errorMessage ?: "Feil fra Saf: ${re.response.bodyAsText()}")

                throw IkkeTilgangTilJournalpost()
            } else if (re.response.status == HttpStatusCode.NotFound) {
                throw IkkeFunnetException(
                    code = "JOURNALPOST_IKKE_FUNNET",
                    detail =
                        "Dokument med journalpostId=$journalpostId, dokumentInfoId=$dokumentInfoId, " +
                            "variantFormat=ARKIV ikke funnet i Joark",
                )
            } else {
                throw ForespoerselException(
                    status = re.response.status.value,
                    code = "UKJENT_FEIL_HENT_JOURNALPOST_PDF",
                    detail = "Ukjent feil oppsto ved henting av journalpost",
                )
            }
        }

    // TODO: Fjerne param [visTemaPen] når gjenlevendepensjon er borte
    suspend fun hentDokumenter(
        fnr: String,
        visTemaPen: Boolean,
        idType: BrukerIdType,
        bruker: BrukerTokenInfo,
    ): DokumentoversiktBrukerResponse {
        logger.info("VisTemaPen=$visTemaPen")

        val request =
            GraphqlRequest(
                query = getQuery("/graphql/dokumentoversiktBruker.graphql"),
                variables =
                    DokumentOversiktBrukerVariables(
                        brukerId =
                            BrukerId(
                                id = fnr,
                                type = idType,
                            ),
                        tema = if (visTemaPen) listOf("EYO", "EYB", "PEN") else listOf("EYO", "EYB"),
                        // TODO: Finn en grense eller fiks paginering
                        foerste = 20,
                    ),
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

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }

    private suspend fun getOboToken(bruker: BrukerTokenInfo): String {
        val token =
            azureAdClient.getOnBehalfOfAccessTokenForResource(
                listOf(safScope),
                bruker.accessToken(),
            )
        return token.get()?.accessToken ?: ""
    }
}
