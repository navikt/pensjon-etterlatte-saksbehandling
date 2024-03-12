package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessage
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(DownstreamResourceClient::class.java)

class DownstreamResourceClient(
    private val azureAdClient: AzureAdClient,
    private val httpClient: HttpClient = defaultHttpClient,
) {
    suspend fun get(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { oboAccessToken ->
                fetchFromDownstreamApi(resource, oboAccessToken)
            }
            .andThen { response ->
                when (response) {
                    null -> Ok(resource)
                    else -> Ok(resource.addResponse(response))
                }
            }
    }

    suspend fun post(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: Any,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { token ->
                postToDownstreamApi(resource, token, postBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun put(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        putBody: Any,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { token ->
                putToDownstreamApi(resource, token, putBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun delete(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: String,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { oboAccessToken ->
                deleteToDownstreamApi(resource, oboAccessToken, postBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun patch(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        patchBody: String,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { oboAccessToken ->
                patchToDownstreamApi(resource, oboAccessToken, patchBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    private suspend fun fetchFromDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
    ): Result<JsonNode?, Throwable> =

        runCatching {
            httpClient.get(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            }
        }
            .fold(
                onSuccess = { response ->
                    when {
                        response.status == HttpStatusCode.NoContent -> Ok(null)
                        response.status.isSuccess() -> Ok(response.body())
                        else -> response.toResponseException()
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun postToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        postBody: Any,
    ): Result<Any, Throwable> =

        runCatching {
            httpClient.post(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }
            .fold(
                onSuccess = { response ->
                    when {
                        response.status.isSuccess() -> {
                            if (response.harContentType(ContentType.Application.Json)) {
                                Ok(response.body<JsonNode>())
                            } else {
                                logger.info("Mottok content-type: ${response.contentType()} som ikke var JSON")
                                Ok(response.status)
                            }
                        }
                        else -> response.toResponseException()
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun putToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        putBody: Any,
    ): Result<Any, Throwable> =

        runCatching {
            httpClient.put(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(putBody)
            }
        }
            .fold(
                onSuccess = { response ->
                    when {
                        response.status.isSuccess() -> {
                            if (response.harContentType(ContentType.Application.Json)) {
                                Ok(response.body<JsonNode>())
                            } else {
                                logger.info("Mottok content-type: ${response.contentType()} som ikke var JSON")
                                Ok(response.status)
                            }
                        }
                        else -> response.toResponseException()
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun deleteToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        postBody: String,
    ): Result<JsonNode?, Throwable> =

        runCatching {
            httpClient.delete(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }
            .fold(
                onSuccess = { response ->
                    when {
                        response.status == HttpStatusCode.NoContent -> Ok(null)
                        response.status.isSuccess() -> Ok(response.body())
                        else -> response.toResponseException()
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun patchToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        patchBody: String,
    ): Result<JsonNode, Throwable> =
        runCatching {
            httpClient.patch(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(patchBody)
            }
        }
            .fold(
                onSuccess = { response ->
                    when {
                        response.status.isSuccess() -> Ok(response.body())
                        else -> response.toResponseException()
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )
}

/**
 * Ktor med content negotiation serialiserer content-type med parametere (som charset), slik at en
 * naiv sammenligning ikke fungerer. Denne metoden sammenligner uten parametere.
 */
private fun HttpMessage?.harContentType(contentType: ContentType): Boolean {
    return contentTypeErLik(this?.contentType(), contentType)
}

fun contentTypeErLik(
    contentEn: ContentType?,
    contentTo: ContentType?,
): Boolean {
    return contentEn?.withoutParameters() == contentTo?.withoutParameters()
}
