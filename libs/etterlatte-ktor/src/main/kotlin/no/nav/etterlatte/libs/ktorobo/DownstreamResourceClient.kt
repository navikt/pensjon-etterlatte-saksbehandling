package no.nav.etterlatte.libs.ktorobo

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
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
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.get(resource.url) {
            bearerAuth(token.accessToken)
            resource.additionalHeaders?.forEach { headers.append(it.key, it.value) }
        }
    }

    suspend fun post(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: Any,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.post(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
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
            setBody(putBody)
        }
    }

    suspend fun delete(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        postBody: String,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.delete(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            setBody(postBody)
        }
    }

    suspend fun patch(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        patchBody: String,
    ) = medToken(resource, brukerTokenInfo) { token ->
        httpClient.patch(resource.url) {
            bearerAuth(token.accessToken)
            contentType(ContentType.Application.Json)
            setBody(patchBody)
        }
    }

    private suspend fun medToken(
        resource: Resource,
        brukerTokenInfo: BrukerTokenInfo,
        action: suspend (token: AccessToken) -> HttpResponse,
    ): Result<Resource, Throwable> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient.hentTokenFraAD(brukerTokenInfo, scopes)
            .andThen { runCatching { action(it) }.fold(resource) }
            .andThen { response ->
                when (response) {
                    null -> Ok(resource)
                    else -> Ok(resource.addResponse(response))
                }
            }
    }

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
