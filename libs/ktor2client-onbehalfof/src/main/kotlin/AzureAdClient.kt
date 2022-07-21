package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.*
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(AzureAdClient::class.java)

internal val defaultHttpClient = HttpClient() {
    expectSuccess = true
    install(ContentNegotiation){
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

data class AzureAdOpenIdConfiguration(
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("issuer")
    val issuer: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String
)

class AzureAdClient(
    private val config: Config,
    private val httpClient: HttpClient = defaultHttpClient
) {
    val openIdConfiguration: AzureAdOpenIdConfiguration = runBlocking {
        defaultHttpClient.get(config.getString("azure.app.well.known.url")).body()
    }

    private suspend inline fun fetchAccessToken(formParameters: Parameters): Result<AccessToken, ThrowableErrorMessage> =
        runCatching {
            httpClient.submitForm(
                url = openIdConfiguration.tokenEndpoint,
                formParameters = formParameters
            ).body<AccessToken>()
        }.fold(
            onSuccess = { result -> Ok(result) },
            onFailure = { error -> error.handleError("Could not fetch access token from authority endpoint") }
        )

    private suspend inline fun get(url: String, oboAccessToken: AccessToken): Result<JsonNode, ThrowableErrorMessage> =
        runCatching {
            httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
            }.body<JsonNode>()
        }.fold(
            onSuccess = { result -> Ok(result) },
            onFailure = { error -> error.handleError("Could not GET $url") }
        )

    private suspend fun Throwable.handleError(message: String): Err<ThrowableErrorMessage> {
        val responseBody: String? = when (this) {
            is ResponseException -> this.response?.bodyAsText()
            else -> null
        }
        return "$message. response body: $responseBody"
            .also { errorMessage -> logger.error(errorMessage, this) }
            .let { errorMessage -> Err(ThrowableErrorMessage(errorMessage, this)) }
    }

    // Service-to-service access token request (client credentials grant)
    suspend fun getAccessTokenForResource(scopes: List<String>): Result<AccessToken, ThrowableErrorMessage> =
        fetchAccessToken(
            Parameters.build {
                append("client_id", config.getString("azure.app.client.id"))
                append("client_secret", config.getString("azure.app.client.secret"))
                append("scope", scopes.joinToString(separator = " "))
                append("grant_type", "client_credentials")
            }
        )

    // Service-to-service access token request (on-behalf-of flow)
    suspend fun getOnBehalfOfAccessTokenForResource(scopes: List<String>, accessToken: String): Result<AccessToken, ThrowableErrorMessage> =
        fetchAccessToken(
            Parameters.build {
                append("client_id", config.getString("azure.app.client.id"))
                append("client_secret", config.getString("azure.app.client.secret"))
                append("scope", scopes.joinToString(separator = " "))
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("requested_token_use", "on_behalf_of")
                append("assertion", accessToken)
                append("assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            }
        )

    // Graph API lookup (on-behalf-of flow)
    suspend fun getUserInfoFromGraph(accessToken: String): Result<JsonNode, ThrowableErrorMessage> {
        val queryProperties = "onPremisesSamAccountName,displayName,givenName,mail,officeLocation,surname,userPrincipalName,id,jobTitle"
        val url = "https://graph.microsoft.com/v1.0/me?\$select=$queryProperties"
        val scopes = listOf("https://graph.microsoft.com/.default")
        return getOnBehalfOfAccessTokenForResource(scopes, accessToken)
            .andThen { oboAccessToken -> get(url, oboAccessToken) }
    }
}

data class AccessToken(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("token_type")
    val tokenType: String
)