package no.nav.etterlatte

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.Parameters
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.etterlatte.libs.common.objectMapper
import java.time.Instant

data class StsTokenConfig(
    val name: String,
    val password: String,
    val url: String
)

class StsClient(private val config: StsTokenConfig) {
    private val httpClient = HttpClient(OkHttp) {
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(config.name, config.password)
                }

                sendWithoutRequest { true }
            }
        }
        install(ContentNegotiation) {
            jackson { objectMapper }
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }

    private val tokenLifetimeMargin = 60
    private var cachedToken: StsToken = StsToken("", "", 0)
    private var cachedTokenExpiery: Instant = Instant.MIN
    private val mutex = Mutex()

    private suspend fun fetchToken(): StsToken = httpClient.submitForm(
        formParameters = Parameters.build {
            append("grant_type", "client_credentials")
            append("scope", "openid")
        },
        url = config.url,
    ).body()

    private suspend fun refreshIfNeeded() {
        Instant.now().also { start ->
            if (cachedTokenExpiery.isBefore(start))
                mutex.withLock {
                    if (cachedTokenExpiery.isBefore(start)) {
                        fetchToken().also {
                            cachedToken = it
                            cachedTokenExpiery = start.plusSeconds(it.expiresIn.toLong() - tokenLifetimeMargin)
                        }
                    }
                }
        }
    }

    suspend fun getToken(): StsToken {
        refreshIfNeeded()
        return cachedToken
    }
}

data class StsToken(
    @JsonProperty(value = "access_token")
    val accessToken: String,
    @JsonProperty(value = "token_type")
    val tokenType: String,
    @JsonProperty(value = "expires_in")
    val expiresIn: Int
) {
    override fun toString() = accessToken
}