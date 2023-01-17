package no.nav.etterlatte.libs.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

fun httpClient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

fun httpClientClientCredentials(scope: String) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(Auth) {
        clientCredential {
            config = mapOf("AZURE_APP_OUTBOUND_SCOPE" to scope)
        }
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }