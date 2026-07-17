package no.nav.etterlatte.testdata.features.prosessering

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.Instant

/**
 * DTO speiler `etterlatte-behandling` sin `ProsesseringTaskDto` — vår rene 5-status-modell,
 * ingen `Ressurs`-konvolutt. `status`/`stoppaarsak` holdes som `String` her slik at testdata
 * ikke trenger å avhenge av prosessering-core.
 */
data class ProsesseringTaskDto(
    val id: Long,
    val type: String,
    val status: String,
    val antallFeil: Int,
    val stoppaarsak: String?,
    val triggerTid: Instant,
    val opprettetTid: Instant,
    val plukketTid: Instant?,
    val payload: String?,
)

/**
 * Kaller prosessering-admin-API-et i `etterlatte-behandling` med saksbehandlerens token
 * via on-behalf-of (samme mønster som resten av testdata mot Gjenny-tjenester). Ren lesing
 * pluss manuell «rekjør» — GUIet ([ProsesseringFeature]) observerer og styrer, motoren kjører.
 */
class ProsesseringKlient(
    config: Config,
    private val httpClient: HttpClient,
    private val azureAdClient: AzureAdClient,
) {
    private val baseUrl = "${config.getString("behandling.app.url")}/api/prosessering/task"
    private val scope = config.getString("behandling.app.scope")

    suspend fun hentTasks(
        bruker: BrukerTokenInfo,
        status: String?,
        limit: Int = 100,
    ): List<ProsesseringTaskDto> {
        val token = token(bruker)
        val url =
            URLBuilder(baseUrl)
                .apply {
                    if (!status.isNullOrBlank()) parameters.append("status", status)
                    parameters.append("limit", limit.toString())
                }.buildString()
        return httpClient
            .get {
                url(url)
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
    }

    suspend fun hentTask(
        bruker: BrukerTokenInfo,
        id: Long,
    ): ProsesseringTaskDto? {
        val token = token(bruker)
        return try {
            httpClient
                .get {
                    url("$baseUrl/$id")
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }
    }

    suspend fun rekjor(
        bruker: BrukerTokenInfo,
        id: Long,
    ) {
        val token = token(bruker)
        httpClient.put {
            url("$baseUrl/$id/rekjor")
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    private suspend fun token(bruker: BrukerTokenInfo): String =
        azureAdClient
            .hentTokenFraAD(bruker, listOf(scope))
            .get()
            ?.accessToken
            ?: error("Klarte ikke hente token for kall mot prosessering-admin-API")
}
