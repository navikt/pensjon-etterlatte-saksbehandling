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
        brukerTokenInfo: BrukerTokenInfo,
    ): Result<Resource, Throwable> {
        val action: suspend (token: AccessToken) -> Result<JsonNode?, Throwable> = { token -> fetchFromDownstreamApi(resource, token) }

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

    suspend fun post(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: Any,
    ): Result<Resource, Throwable> {
        val action: suspend (
            token: AccessToken,
        ) -> Result<JsonNode?, Throwable> = { token -> postToDownstreamApi(resource, token, postBody) }
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

    suspend fun put(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        putBody: Any,
    ): Result<Resource, Throwable> {
        val action: suspend (token: AccessToken) -> Result<JsonNode?, Throwable> = { token -> putToDownstreamApi(resource, token, putBody) }
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

    suspend fun delete(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: String,
    ): Result<Resource, Throwable> {
        val action: suspend (
            token: AccessToken,
        ) -> Result<JsonNode?, Throwable> = { token -> deleteToDownstreamApi(resource, token, postBody) }

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

    suspend fun patch(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        patchBody: String,
    ): Result<Resource, Throwable> {
        val action: suspend (
            token: AccessToken,
        ) -> Result<JsonNode?, Throwable> = { token -> patchToDownstreamApi(resource, token, patchBody) }
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { action(it) }
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
                onSuccess = { haandterRespons(it) },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun postToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        postBody: Any,
    ): Result<JsonNode?, Throwable> =

        runCatching {
            httpClient.post(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }
            .fold(
                onSuccess = { haandterRespons(it) },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun putToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        putBody: Any,
    ): Result<JsonNode?, Throwable> =

        runCatching {
            httpClient.put(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(putBody)
            }
        }
            .fold(
                onSuccess = { haandterRespons(it) },
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
                onSuccess = { haandterRespons(it) },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )

    private suspend fun haandterRespons(response: HttpResponse) =
        when {
            response.status == HttpStatusCode.NoContent -> Ok(null)
            response.status.isSuccess() -> Ok(response.body())
            else -> response.toResponseException()
        }

    private suspend fun patchToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        patchBody: String,
    ): Result<JsonNode?, Throwable> =
        runCatching {
            httpClient.patch(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(patchBody)
            }
        }
            .fold(
                onSuccess = { haandterRespons(it) },
                onFailure = { error ->
                    error.toErr(resource.url)
                },
            )
}
