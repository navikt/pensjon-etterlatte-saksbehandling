package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(AzureAdClient::class.java)

internal val defaultHttpClient = HttpClient() {
    expectSuccess = true
    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private val httpClient: HttpClient = defaultHttpClient,
    private val cache: AsyncCache<OboTokenRequest, AccessToken> = Caffeine
        .newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .buildAsync(),
    private val clientCredentialsCache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> = Caffeine
        .newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .buildAsync()
) {
    private val openIdConfiguration: AzureAdOpenIdConfiguration = runBlocking {
        httpClient.get(config.getString("azure.app.well.known.url")).body()
    }

    private suspend inline fun fetchAccessToken(formParameters: Parameters): AccessToken =
        try {
            httpClient.submitForm(
                url = openIdConfiguration.tokenEndpoint,
                formParameters = formParameters
            ).body()
        } catch (ex: Throwable) {
            val responseBody: String? = when (ex) {
                is ResponseException -> ex.response.bodyAsText()
                else -> null
            }
            throw RuntimeException(
                "Could not fetch access token from authority endpoint. response body: $responseBody",
                ex
            )
        }

    private suspend inline fun get(url: String, oboAccessToken: AccessToken): Result<JsonNode, ThrowableErrorMessage> =
        runCatching {
            httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer ${oboAccessToken.accessToken}")
            }
        }.mapCatching { response ->
            response.checkForError()
        }.fold(
            onSuccess = { result -> Ok(result.body()) },
            onFailure = { error ->
                val downstreamResourceClient = when (error) {
                    is HttpStatusRuntimeException -> error.downstreamStatusCode
                    else -> null
                }

                error.handleError("Could not GET $url", downstreamResourceClient)
            }
        )

    private suspend fun Throwable.handleError(message: String, downstreamResourceClient: HttpStatusCode?):
        Err<ThrowableErrorMessage> {
        val responseBody: String? = when (this) {
            is ResponseException -> this.response.bodyAsText()
            else -> null
        }
        return "$message. response body: $responseBody"
            .also { errorMessage -> logger.error(errorMessage, this) }
            .let { errorMessage -> Err(ThrowableErrorMessage(errorMessage, this, downstreamResourceClient)) }
    }

    // Service-to-service access token request (client credentials grant)
    suspend fun getAccessTokenForResource(scopes: List<String>): Result<AccessToken, ThrowableErrorMessage> {
        val params = { req: ClientCredentialsTokenRequest ->
            Parameters.build {
                append("client_id", config.getString("azure.app.client.id"))
                append("client_secret", config.getString("azure.app.client.secret"))
                append("scope", scopes.joinToString(separator = " "))
                append("grant_type", "client_credentials")
            }
        }
        return hentAccessToken(params, clientCredentialsCache, ClientCredentialsTokenRequest(scopes))
    }

    private suspend fun <T : TokenRequest> hentAccessToken(
        params: (T) -> Parameters,
        asyncCache: AsyncCache<T, AccessToken>,
        request: T
    ): Result<AccessToken, ThrowableErrorMessage> {
        val context = currentCoroutineContext()

        val value = asyncCache.get(request) { req, _ ->
            CoroutineScope(context).future {
                fetchAccessToken(params.invoke(req))
            }
        }

        return value.handle { token, exception ->
            if (exception != null) {
                Err(ThrowableErrorMessage("Henting av token feilet", exception))
            } else {
                Ok(token)
            }
        }.asDeferred()
            .await()
    }

    // Service-to-service access token request (on-behalf-of flow)
    suspend fun getOnBehalfOfAccessTokenForResource(
        scopes: List<String>,
        accessToken: String
    ): Result<AccessToken, ThrowableErrorMessage> {
        val params = { req: OboTokenRequest ->
            Parameters.build {
                append("client_id", config.getString("azure.app.client.id"))
                append("client_secret", config.getString("azure.app.client.secret"))
                append("scope", req.scopes.joinToString(separator = " "))
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("requested_token_use", "on_behalf_of")
                append("assertion", req.accessToken)
                append("assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            }
        }
        return hentAccessToken(params, cache, OboTokenRequest(scopes, accessToken))
    }

    // Graph API lookup (on-behalf-of flow)
    suspend fun getUserInfoFromGraph(accessToken: String): Result<JsonNode, ThrowableErrorMessage> {
        val queryProperties =
            "onPremisesSamAccountName,displayName,givenName,mail,officeLocation,surname,userPrincipalName,id,jobTitle"
        val url = "https://graph.microsoft.com/v1.0/me?\$select=$queryProperties"
        val scopes = listOf("https://graph.microsoft.com/.default")
        return getOnBehalfOfAccessTokenForResource(scopes, accessToken)
            .andThen { oboAccessToken -> get(url, oboAccessToken) }
    }
}