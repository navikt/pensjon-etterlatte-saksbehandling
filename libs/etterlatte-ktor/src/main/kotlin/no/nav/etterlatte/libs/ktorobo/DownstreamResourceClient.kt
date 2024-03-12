package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.token.BrukerTokenInfo

class DownstreamResourceClient(
    private val azureAdClient: AzureAdClient,
    private val httpClient: HttpClient = defaultHttpClient,
) {
    suspend fun get(
        resource: Resource,
        bruker: BrukerTokenInfo,
    ) = gjoerKall(resource, bruker) { token ->
        runCatching {
            httpClient.get(resource.url) {
                header(token)
                resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            }
        }.fold(resource)
    }

    suspend fun post(
        resource: Resource,
        bruker: BrukerTokenInfo,
        postBody: Any,
    ) = gjoerKall(resource, bruker) { token ->
        runCatching {
            httpClient.post(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }.fold(resource)
    }

    suspend fun put(
        resource: Resource,
        bruker: BrukerTokenInfo,
        putBody: Any,
    ) = gjoerKall(resource, bruker) { token ->
        runCatching {
            httpClient.put(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(putBody)
            }
        }.fold(resource)
    }

    suspend fun delete(
        resource: Resource,
        bruker: BrukerTokenInfo,
        postBody: String,
    ) = gjoerKall(resource, bruker) { token ->
        runCatching {
            httpClient.delete(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }.fold(resource)
    }

    suspend fun patch(
        resource: Resource,
        bruker: BrukerTokenInfo,
        patchBody: String,
    ) = gjoerKall(resource, bruker) { token ->
        runCatching {
            httpClient.patch(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(patchBody)
            }
        }.fold(resource)
    }

    private suspend fun gjoerKall(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        action: suspend (token: AccessToken) -> Result<JsonNode?, Throwable>,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes).andThen { action(it) }.andThen { response ->
            when (response) {
                null -> Ok(resource)
                else -> Ok(resource.addResponse(response))
            }
        }
    }

    private fun HttpRequestBuilder.header(token: AccessToken) = header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")

    private suspend fun kotlin.Result<HttpResponse>.fold(resource: Resource) =
        this.fold(
            onSuccess = { response ->
                when {
                    response.status == HttpStatusCode.NoContent -> Ok(null)
                    response.status.isSuccess() -> Ok(response.body())
                    else -> response.toResponseException()
                }
            },
            onFailure = { error -> error.toErr(resource.url) },
        )
}
