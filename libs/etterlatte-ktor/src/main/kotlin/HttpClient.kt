package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.NAV_CALL_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

fun httpClient(forventSuksess: Boolean = false) = httpClientMedAuth(forventSuksess)

fun httpClientClientCredentials(
    azureAppClientId: String,
    azureAppJwk: String,
    azureAppWellKnownUrl: String,
    azureAppScope: String,
    ekstraJacksoninnstillinger: ((o: ObjectMapper) -> Unit) = { }
) = httpClientMedAuth(
    ekstraJacksoninnstillinger = ekstraJacksoninnstillinger,
    auth = {
        it.install(Auth) {
            clientCredential {
                config = mapOf(
                    "AZURE_APP_CLIENT_ID" to azureAppClientId,
                    "AZURE_APP_JWK" to azureAppJwk,
                    "AZURE_APP_WELL_KNOWN_URL" to azureAppWellKnownUrl,
                    "AZURE_APP_OUTBOUND_SCOPE" to azureAppScope
                )
            }
        }
    },
    forventSuksess = true
)

private fun httpClientMedAuth(
    forventSuksess: Boolean,
    ekstraJacksoninnstillinger: (o: ObjectMapper) -> Unit = { },
    auth: (cl: HttpClientConfig<OkHttpConfig>) -> Unit? = {}
) = HttpClient(OkHttp) {
    expectSuccess = forventSuksess
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
        ekstraJacksoninnstillinger(objectMapper)
    }
    auth.invoke(this)
    defaultRequest {
        header(HttpHeaders.XCorrelationId, getCorrelationId())
        header(NAV_CALL_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }