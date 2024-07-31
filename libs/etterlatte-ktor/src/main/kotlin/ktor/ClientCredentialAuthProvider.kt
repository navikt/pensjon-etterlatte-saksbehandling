package no.nav.etterlatte.libs.ktor.ktor

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_OUTBOUND_SCOPE
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2CacheFactory
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import java.net.URI
import java.time.Duration

fun Auth.clientCredential(block: ClientCredentialAuthConfig.() -> Unit) {
    with(ClientCredentialAuthConfig().apply(block)) {
        providers.add(ClientCredentialAuthProvider(config))
    }
}

class ClientCredentialAuthConfig {
    lateinit var config: Miljoevariabler
}

class ClientCredentialAuthProvider(
    config: Miljoevariabler,
) : AuthProvider {
    @Deprecated("Please use sendWithoutRequest function instead")
    override val sendWithoutRequest: Boolean = true

    private val clientPropertiesConfig =
        ClientProperties(
            // URI(conf["token_endpoint_url"]!!),
            tokenEndpointUrl = null,
            wellKnownUrl = config[AZURE_APP_WELL_KNOWN_URL]?.let { URI(it) },
            grantType = GrantType.CLIENT_CREDENTIALS,
            scope = config[AZURE_APP_OUTBOUND_SCOPE]?.split(",") ?: emptyList(),
            authentication =
                ClientAuthenticationProperties
                    .builder(
                        clientId = config.getOrThrow(AZURE_APP_CLIENT_ID),
                        clientAuthMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                    ).clientJwk(config.getOrThrow(AZURE_APP_JWK))
                    .build(),
            // conf["resource_url"]?.let { URI(it) },
            resourceUrl = null,
            tokenExchange = null,
        )

    private fun Miljoevariabler.getOrThrow(key: AzureEnums) =
        this[key]
            ?: throw IllegalArgumentException("Missing configuration property '$key'")

    private val httpClient = DefaultOAuth2HttpClient()
    private val accessTokenService = setupOAuth2AccessTokenService(httpClient = httpClient)

    override fun isApplicable(auth: HttpAuthHeader): Boolean = true

    override suspend fun addRequestHeaders(
        request: HttpRequestBuilder,
        authHeader: HttpAuthHeader?,
    ) {
        accessTokenService.getAccessToken(clientPropertiesConfig).accessToken.also {
            request.headers[HttpHeaders.Authorization] = "Bearer $it"
        }
    }
}

internal fun setupOAuth2AccessTokenService(httpClient: DefaultOAuth2HttpClient): OAuth2AccessTokenService =
    OAuth2AccessTokenService(
        tokenResolver = { throw IllegalArgumentException("Skal ikke kalle denne") },
        onBehalfOfTokenClient = OnBehalfOfTokenClient(httpClient),
        clientCredentialsTokenClient = ClientCredentialsTokenClient(httpClient),
        clientCredentialsGrantCache =
            OAuth2CacheFactory.accessTokenResponseCache(
                10,
                Duration.ofMinutes(50L).toSeconds(),
            ),
        tokenExchangeClient = TokenExchangeClient(httpClient),
    )
