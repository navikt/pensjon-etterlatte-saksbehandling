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
    ): Result<Resource, Throwable> = gjoerKall(resource, bruker) { token -> fetchFromDownstreamApi(resource, token) }

    suspend fun post(
        resource: Resource,
        bruker: BrukerTokenInfo,
        postBody: Any,
    ): Result<Resource, Throwable> = gjoerKall(resource, bruker) { token -> postToDownstreamApi(resource, token, postBody) }

    suspend fun put(
        resource: Resource,
        bruker: BrukerTokenInfo,
        putBody: Any,
    ): Result<Resource, Throwable> = gjoerKall(resource, bruker) { token -> putToDownstreamApi(resource, token, putBody) }

    suspend fun delete(
        resource: Resource,
        bruker: BrukerTokenInfo,
        postBody: String,
    ): Result<Resource, Throwable> = gjoerKall(resource, bruker) { token -> deleteToDownstreamApi(resource, token, postBody) }

    suspend fun patch(
        resource: Resource,
        bruker: BrukerTokenInfo,
        patchBody: String,
    ): Result<Resource, Throwable> = gjoerKall(resource, bruker) { token -> patchToDownstreamApi(resource, token, patchBody) }

    private suspend fun gjoerKall(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        action: suspend (token: AccessToken) -> Result<JsonNode?, Throwable>,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { action(it) }
            .andThen { response ->
                when (response) {
                    null -> Ok(resource)
                    else -> Ok(resource.addResponse(response))
                }
            }
    }

    private suspend fun fetchFromDownstreamApi(
        resource: Resource,
        token: AccessToken,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.get(resource.url) {
                header(token)
                resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
            }
        }.fold(resource)

    private suspend fun postToDownstreamApi(
        resource: Resource,
        token: AccessToken,
        postBody: Any,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.post(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }.fold(resource)

    private suspend fun putToDownstreamApi(
        resource: Resource,
        token: AccessToken,
        putBody: Any,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.put(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(putBody)
            }
        }.fold(resource)

    private fun HttpRequestBuilder.header(token: AccessToken) = header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")

    private suspend fun kotlin.Result<HttpResponse>.fold(resource: Resource) =
        this.fold(
            onSuccess = { haandterRespons(it) },
            onFailure = { error -> error.toErr(resource.url) },
        )

    private suspend fun deleteToDownstreamApi(
        resource: Resource,
        token: AccessToken,
        postBody: String,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.delete(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }.fold(resource)

    private suspend fun haandterRespons(response: HttpResponse) =
        when {
            response.status == HttpStatusCode.NoContent -> Ok(null)
            response.status.isSuccess() -> Ok(response.body())
            else -> response.toResponseException()
        }

    private suspend fun patchToDownstreamApi(
        resource: Resource,
        token: AccessToken,
        patchBody: String,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.patch(resource.url) {
                header(token)
                contentType(ContentType.Application.Json)
                setBody(patchBody)
            }
        }.fold(resource)
}
