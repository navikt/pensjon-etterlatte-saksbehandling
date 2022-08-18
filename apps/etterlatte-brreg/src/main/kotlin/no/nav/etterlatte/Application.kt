package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.enhetsregister.EnhetsregKlient
import no.nav.etterlatte.enhetsregister.EnhetsregService
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper

class ApplicationContext {
    private val env = System.getenv()

    val service = EnhetsregService(EnhetsregKlient(env["BRREG_URL"]!!, httpClient()))

    private fun httpClient() = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }
}

fun main() {
    ApplicationContext().also {
        Server(it).run()
    }
}