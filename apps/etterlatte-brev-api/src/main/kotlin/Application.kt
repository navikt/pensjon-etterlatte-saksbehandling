package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper

class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()
    val brevService = BrevService(httpClient(), config.getString("pdfgen.url"))

    private fun httpClient() = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}

fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
