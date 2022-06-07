package no.nav.etterlatte


import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper

class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val vedtakKlient = VedtakKlient(config, httpClient())
    val behandlingService: BehandlingService = BehandlingService(
        behandlingKlient = behandlingKlient,
        pdlKlient = PdltjenesterKlient(config, httpClient()),
        grunnlagKlient = GrunnlagKlient(config, httpClient()),
        vedtakKlient = vedtakKlient,
    )

    val oppgaveService: OppgaveService = OppgaveService(behandlingKlient)
    val vedtakService = VedtakService(behandlingKlient, vedtakKlient)

    private fun httpClient() = HttpClient {
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
