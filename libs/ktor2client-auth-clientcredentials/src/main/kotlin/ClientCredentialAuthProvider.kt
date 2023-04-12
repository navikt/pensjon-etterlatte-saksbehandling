package no.nav.etterlatte.security.ktor

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.HttpAuthHeader
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2CacheFactory
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import java.net.URI
import java.time.Duration

fun Auth.clientCredential(block: ClientCredentialAuthConfig.() -> Unit) {
    with(ClientCredentialAuthConfig().apply(block)) {
        providers.add(ClientCredentialAuthProvider(config))
    }
}

class ClientCredentialAuthConfig {
    lateinit var config: Map<String, String>
}

class ClientCredentialAuthProvider(config: Map<String, String>) : AuthProvider {
    override val sendWithoutRequest: Boolean = true

    private val clientPropertiesConfig = ClientProperties(
        null, // URI(conf["token_endpoint_url"]!!),
        config["AZURE_APP_WELL_KNOWN_URL"]?.let { URI(it) },
        OAuth2GrantType("client_credentials"),
        config["AZURE_APP_OUTBOUND_SCOPE"]?.split(",") ?: emptyList(),
        ClientAuthenticationProperties(
            config["AZURE_APP_CLIENT_ID"],
            ClientAuthenticationMethod.PRIVATE_KEY_JWT,
            null,
            config["AZURE_APP_JWK"]
        ),
        null, // conf["resource_url"]?.let { URI(it) },
        null
    )
    private val httpClient = DefaultOAuth2HttpClient()
    private val accessTokenService = setupOAuth2AccessTokenService(httpClient = httpClient)

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return true
    }

    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?) {
        accessTokenService.getAccessToken(clientPropertiesConfig).accessToken.also {
            request.headers[HttpHeaders.Authorization] = "Bearer $it"
        }
    }
}

internal fun setupOAuth2AccessTokenService(httpClient: DefaultOAuth2HttpClient): OAuth2AccessTokenService {
    return OAuth2AccessTokenService(
        null,
        null,
        ClientCredentialsTokenClient(httpClient),
        null
    ).also {
        it.clientCredentialsGrantCache = OAuth2CacheFactory.accessTokenResponseCache(
            30,
            Duration.ofMinutes(50L).toSeconds()
        )
    }
}