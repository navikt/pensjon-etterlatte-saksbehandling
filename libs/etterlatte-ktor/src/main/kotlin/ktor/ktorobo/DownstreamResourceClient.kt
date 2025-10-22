package no.nav.etterlatte.libs.ktor.ktor.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMessage
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.ktor.navConsumerId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class DownstreamResourceClient(
    private val azureAdClient: AzureAdClient,
    private val httpClient: HttpClient = defaultHttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun get(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        timeoutConfig: (HttpTimeoutConfig.() -> Unit)? = null,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.get(resource.url) {
            bearerAuth(token.accessToken)
            timeoutConfig?.let { config ->
                timeout {
                    apply(config)
                }
            }

            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
        }
    }

    internal suspend fun getUtenToken(
        url: String,
        konsument: String,
    ) = httpClient.get(url) {
        accept(ContentType.Application.Json)
        navConsumerId(konsument)
    }

    suspend fun post(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: Any,
        timeoutConfig: (HttpTimeoutConfig.() -> Unit)? = null,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.post(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            timeoutConfig?.let { config ->
                timeout {
                    apply(config)
                }
            }
            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            setBody(postBody)
        }
    }

    suspend fun put(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        putBody: Any,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.put(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            setBody(putBody)
        }
    }

    suspend fun delete(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: String? = "",
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.delete(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            setBody(postBody)
        }
    }

    suspend fun patch(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        patchBody: Any? = null,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.patch(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            setBody(patchBody)
        }
    }

    private suspend fun medToken(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        action: suspend (token: AccessToken) -> HttpResponse,
    ) = azureAdClient
        .hentTokenFraAD(brukerTokenInfo, listOf("api://${resource.clientId}/.default"))
        .andThen { runCatching { action(it) }.fold(resource) }
        .andThen { response ->
            when (response) { // TODO Hvorfor skal alt være Ok her?
                null -> Ok(resource)
                else -> Ok(resource.addResponse(response))
            }
        }

    private suspend fun Result<HttpResponse>.fold(resource: Resource) =
        this.fold(
            onSuccess = { response ->
                resource.addStatusCode(response.status)
                when {
                    response.status == HttpStatusCode.NoContent -> Ok(null)
                    response.status.isSuccess() -> {
                        if (response.harContentType(ContentType.Application.Json)) {
                            Ok(response.body<JsonNode>())
                        } else if (response.harContentType(ContentType.Text.Plain)) {
                            Ok(response.body<String>())
                        } else {
                            if (response.contentLength() == 0L) {
                                logger.info("Mottok tom content type${response.contentType()}")
                            } else {
                                logger.warn(
                                    "Mottok uhåndtert content-type: ${response.contentType()} lengde på innhold: ${response.contentLength()} ",
                                )
                            }
                            Ok(null)
                        }
                    }

                    else -> response.toResponseException()
                }
            },
            onFailure = { error -> error.toErr(resource.url) },
        )

    /**
     * Ktor med content negotiation serialiserer content-type med parametere (som charset), slik at en
     * naiv sammenligning ikke fungerer. Denne metoden sammenligner uten parametere.
     */
    private fun HttpMessage?.harContentType(contentType: ContentType): Boolean =
        this?.contentType()?.withoutParameters() == contentType.withoutParameters()
}
