package no.nav.etterlatte.brreg

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.brreg.enhetsregister.EnhetsregKlient
import no.nav.etterlatte.brreg.enhetsregister.EnhetsregService
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper

class ApplicationContext {
    private val env = System.getenv()

    val service = EnhetsregService(EnhetsregKlient(env["BRREG_URL"]!!, httpClient()))

    private fun httpClient() = HttpClient {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
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
