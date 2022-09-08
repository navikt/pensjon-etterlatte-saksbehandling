package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.medl.MedlRegisterKlient

class ApplicationContext {
    private val env = System.getenv()

    val medlService = MedlRegisterKlient(env["MEDL_URL"]!!, httpClient())

    private fun httpClient() = HttpClient {
        install(ContentNegotiation) {
            jackson { objectMapper }
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