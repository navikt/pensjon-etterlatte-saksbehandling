package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessage
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(DownstreamResourceClient::class.java)

class DownstreamResourceClient(
    private val azureAdClient: AzureAdClient,
    private val httpClient: HttpClient = defaultHttpClient
) {
    suspend fun get(
        resource: Resource,
        accessToken: String
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient
            .getOnBehalfOfAccessTokenForResource(scopes, accessToken)
            .andThen { oboAccessToken ->
                fetchFromDownstreamApi(resource, oboAccessToken)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun post(
        resource: Resource,
        accessToken: String,
        postBody: Any
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient
            .getOnBehalfOfAccessTokenForResource(scopes, accessToken)
            .andThen { oboAccessToken ->
                postToDownstreamApi(resource, oboAccessToken, postBody)
            }
            .andThen { response ->
                Ok(resource.addResponse(response))
            }
    }

    suspend fun delete(
        resource: Resource,
        accessToken: String,
        postBody: String
    ): Result<Resource, ThrowableErrorMessage> {
        val scopes = listOf("api://${resource.clientId}/.default")
        return azureAdClient
            .getOnBehalfOfAccessTokenForResource(scopes, accessToken)
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
    ): Result<JsonNode, ThrowableErrorMessage> =

        runCatching {
            httpClient.get(resource.url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
            }.body<JsonNode>()
        }.fold(
            onSuccess = { result ->
                Ok(result)
            },
            onFailure = { error ->
                logger.error("received error from downstream api", error)
                Err(ThrowableErrorMessage(message = "Error response from '${resource.url}'", throwable = error))
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
        }.fold(
            onSuccess = { result ->
                if (result.harContentType(ContentType.Application.Json)) {
                    Ok(result.body<ObjectNode>())
                } else {
                    logger.info("Mottok content-type: ${result.contentType()} som ikke var JSON")
                    Ok(result.status)
                }
            },
            onFailure = { error ->
                logger.error("received error from downstream api", error)
                Err(ThrowableErrorMessage(message = "Error response from '${resource.url}'", throwable = error))
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
            }.body<JsonNode>()
        }.fold(
            onSuccess = { result ->
                Ok(result)
            },
            onFailure = { error ->
                logger.error("received error from downstream api", error)
                Err(ThrowableErrorMessage(message = "Error response from '${resource.url}'", throwable = error))
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
