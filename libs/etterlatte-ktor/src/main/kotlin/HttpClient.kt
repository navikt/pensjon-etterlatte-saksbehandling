package no.nav.etterlatte.libs.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

fun httpClient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    defaultRequest {
        header(HttpHeaders.XCorrelationId, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

fun httpClientClientCredentials(
    azureAppClientId: String,
    azureAppJwk: String,
    azureAppWellKnownUrl: String,
    azureAppScope: String,
    ekstraJacksoninnstillinger: ((o: ObjectMapper) -> Unit) = { }
) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
        ekstraJacksoninnstillinger(objectMapper)
    }
    install(Auth) {
        clientCredential {
            config = mapOf(
                "AZURE_APP_CLIENT_ID" to azureAppClientId,
                "AZURE_APP_JWK" to azureAppJwk,
                "AZURE_APP_WELL_KNOWN_URL" to azureAppWellKnownUrl,
                "AZURE_APP_OUTBOUND_SCOPE" to azureAppScope
            )
        }
    }
    defaultRequest {
        header(HttpHeaders.XCorrelationId, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }