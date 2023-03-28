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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessage
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.SystemBruker
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(DownstreamResourceClient::class.java)

class DownstreamResourceClient(
    private val azureAdClient: AzureAdClient,
    private val httpClient: HttpClient = defaultHttpClient
) {
    suspend fun get(
        resource: Resource,
        bruker: Bruker
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return hentTokenFraAD(bruker, scopes)
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

    private suspend fun hentTokenFraAD(
        bruker: Bruker,
        scopes: List<String>
    ): Result<AccessToken, ThrowableErrorMessage> = if (bruker is SystemBruker) {
        azureAdClient.getAccessTokenForResource(scopes)
    } else {
        azureAdClient
            .getOnBehalfOfAccessTokenForResource(scopes, bruker.accessToken())
    }

    suspend fun post(
        resource: Resource,
        bruker: Bruker,
        postBody: Any
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return hentTokenFraAD(bruker, scopes)
            .andThen { token ->
                postToDownstreamApi(resource, token, postBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun delete(
        resource: Resource,
        bruker: Bruker,
        postBody: String
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return hentTokenFraAD(bruker, scopes)
            .andThen { oboAccessToken ->
                deleteToDownstreamApi(resource, oboAccessToken, postBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    private suspend fun fetchFromDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken
    ): Result<JsonNode?, ThrowableErrorMessage> =

        runCatching {
            httpClient.get(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
            }
        }
            .mapCatching { response ->
                response.checkForError()
            }
            .fold(
                onSuccess = { result ->
                    when (result.status) {
                        HttpStatusCode.NoContent -> Ok(null)
                        else -> Ok(result.body())
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                }
            )

    private suspend fun postToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        postBody: Any
    ): Result<Any, ThrowableErrorMessage> =

        runCatching {
            httpClient.post(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }
            .mapCatching { response ->
                response.checkForError()
            }
            .fold(
                onSuccess = { result ->
                    if (result.harContentType(ContentType.Application.Json)) {
                        Ok(result.body<JsonNode>())
                    } else {
                        logger.info("Mottok content-type: ${result.contentType()} som ikke var JSON")
                        Ok(result.status)
                    }
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                }
            )

    private suspend fun deleteToDownstreamApi(
        resource: Resource,
        oboAccessToken: AccessToken,
        postBody: String
    ): Result<JsonNode, ThrowableErrorMessage> =

        runCatching {
            httpClient.delete(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(postBody)
            }
        }
            .mapCatching { response ->
                response.checkForError()
            }
            .fold(
                onSuccess = { result ->
                    Ok(result.body())
                },
                onFailure = { error ->
                    error.toErr(resource.url)
                }
            )
}

/**
 * Ktor med content negotiation serialiserer content-type med parametere (som charset), slik at en
 * naiv sammenligning ikke fungerer. Denne metoden sammenligner uten parametere.
 */
private fun HttpMessage?.harContentType(contentType: ContentType): Boolean {
    return contentTypeErLik(this?.contentType(), contentType)
}

fun contentTypeErLik(contentEn: ContentType?, contentTo: ContentType?): Boolean {
    return contentEn?.withoutParameters() == contentTo?.withoutParameters()
}