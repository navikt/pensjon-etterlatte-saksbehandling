package no.nav.etterlatte.libs.ktor.ktor.ktorobo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.client.ClientCallLogging
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.util.concurrent.TimeUnit

internal val defaultHttpClient =
    HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
        install(ClientCallLogging)
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
    val authorizationEndpoint: String,
)

interface IAzureAdHttpClient {
    suspend fun doGet(url: String): HttpResponse

    suspend fun submitForm(
        url: String,
        params: Parameters,
    ): HttpResponse
}

class AzureAdHttpClient(
    val httpClient: HttpClient,
) : IAzureAdHttpClient {
    override suspend fun doGet(url: String) = httpClient.get(url)

    override suspend fun submitForm(
        url: String,
        params: Parameters,
    ) = httpClient.submitForm(url, params)
}

class AzureAdClient(
    private val config: Config,
    private val httpClient: IAzureAdHttpClient = AzureAdHttpClient(defaultHttpClient),
    private val cache: AsyncCache<OboTokenRequest, AccessToken> = asyncCache(),
    private val clientCredentialsCache: AsyncCache<ClientCredentialsTokenRequest, AccessToken> =
        asyncCache(),
) {
    private val openIdConfiguration: AzureAdOpenIdConfiguration =
        runBlocking {
            httpClient.doGet(config.getString("azure.app.well.known.url")).body()
        }

    private suspend inline fun fetchAccessToken(formParameters: Parameters): AccessToken =
        try {
            httpClient
                .submitForm(
                    openIdConfiguration.tokenEndpoint,
                    formParameters,
                ).body()
        } catch (ex: Throwable) {
            val responseBody: String? =
                when (ex) {
                    is ResponseException -> ex.response.bodyAsText()
                    else -> null
                }
            throw RuntimeException(
                "Could not fetch access token from authority endpoint. response body: $responseBody",
                ex,
            )
        }

    suspend fun hentTokenFraAD(
        bruker: BrukerTokenInfo,
        scopes: List<String>,
    ) = if (bruker is Systembruker) {
        getAccessTokenForResource(scopes)
    } else {
        getOnBehalfOfAccessTokenForResource(scopes, bruker.accessToken())
    }

    // Service-to-service access token request (client credentials grant)
    internal suspend fun getAccessTokenForResource(scopes: List<String>): Result<AccessToken, ThrowableErrorMessage> {
        val params = { _: ClientCredentialsTokenRequest ->
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
        request: T,
    ): Result<AccessToken, ThrowableErrorMessage> {
        val context = currentCoroutineContext()

        val value =
            asyncCache.get(request) { req, _ ->
                CoroutineScope(context).future {
                    retryOgPakkUt {
                        fetchAccessToken(params.invoke(req))
                    }
                }
            }

        return value
            .handle { token, exception ->
                if (exception != null) {
                    Err(ThrowableErrorMessage("Henting av token feilet", exception))
                } else {
                    Ok(token)
                }
            }.asDeferred()
            .await()
    }

    // Service-to-service access token request (on-behalf-of flow)
    internal suspend fun getOnBehalfOfAccessTokenForResource(
        scopes: List<String>,
        accessToken: String,
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
}

private fun <T : TokenRequest> asyncCache(): AsyncCache<T, AccessToken> =
    Caffeine
        .newBuilder()
        .expireAfter(TokenBasedExpiration())
        .buildAsync()

// Benytte tokenets faktisk expiration i stedet for f.eks. hardkodet, med litt fratrekk som "leeway"
class TokenBasedExpiration : Expiry<TokenRequest, AccessToken> {
    // Fallback 5 sek (som tidligere var normal exp)
    override fun expireAfterCreate(
        key: TokenRequest?,
        value: AccessToken?,
        currentTime: Long,
    ): Long {
        val seconds: Long = value?.expiresIn?.minus(5)?.toLong() ?: 5
        return TimeUnit.SECONDS.toNanos(seconds)
    }

    override fun expireAfterUpdate(
        key: TokenRequest?,
        value: AccessToken?,
        currentTime: Long,
        currentDuration: Long,
    ): Long = currentDuration

    override fun expireAfterRead(
        key: TokenRequest?,
        value: AccessToken?,
        currentTime: Long,
        currentDuration: Long,
    ): Long = currentDuration
}
