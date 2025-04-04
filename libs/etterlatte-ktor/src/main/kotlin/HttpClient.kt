package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.client.ClientCallLogging
import no.nav.etterlatte.libs.ktor.ktor.clientCredential

fun httpClientClientCredentials(
    azureAppClientId: String,
    azureAppJwk: String,
    azureAppWellKnownUrl: String,
    azureAppScope: String,
    forventSuksess: Boolean = true,
    ekstraJacksoninnstillinger: ((o: ObjectMapper) -> Unit) = { },
) = httpClient(
    ekstraJacksoninnstillinger = ekstraJacksoninnstillinger,
    auth = {
        it.install(Auth) {
            clientCredential {
                config =
                    Miljoevariabler.httpClient(
                        mapOf(
                            AzureEnums.AZURE_APP_CLIENT_ID to azureAppClientId,
                            AzureEnums.AZURE_APP_JWK to azureAppJwk,
                            AzureEnums.AZURE_APP_WELL_KNOWN_URL to azureAppWellKnownUrl,
                            AzureEnums.AZURE_APP_OUTBOUND_SCOPE to azureAppScope,
                        ),
                    )
            }
        }
    },
    forventSuksess = forventSuksess,
)

fun httpClient(
    forventSuksess: Boolean = false,
    ekstraJacksoninnstillinger: (o: ObjectMapper) -> Unit = { },
    auth: (cl: HttpClientConfig<OkHttpConfig>) -> Unit? = {},
    ekstraDefaultHeaders: (builder: HttpMessageBuilder) -> Unit = {},
) = HttpClient(OkHttp) {
    expectSuccess = forventSuksess
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
        ekstraJacksoninnstillinger(objectMapper)
    }
    install(ClientCallLogging)
    install(HttpTimeout)

    auth.invoke(this)
    defaultRequest {
        getCorrelationId().let {
            header(HttpHeaders.XCorrelationId, it)
            header("Nav_Call_Id", it)
            header("Nav-Call-Id", it)
        }
        ekstraDefaultHeaders.invoke(this)
    }
}

enum class AzureEnums : EnvEnum {
    AZURE_APP_CLIENT_ID,
    AZURE_APP_JWK,
    AZURE_APP_WELL_KNOWN_URL,
    AZURE_APP_OUTBOUND_SCOPE,
    ;

    override fun key() = name
}
