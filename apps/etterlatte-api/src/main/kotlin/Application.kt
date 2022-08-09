package no.nav.etterlatte


import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper

class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val vedtakKlient = VedtakKlient(config, httpClient())
    private val rapid: KafkaProdusent<String, String> =
        GcpKafkaConfig.fromEnv().standardProducer( System.getenv().getValue("KAFKA_RAPID_TOPIC"))

    val behandlingService: BehandlingService = BehandlingService(
        behandlingKlient = behandlingKlient,
        pdlKlient = PdltjenesterKlient(config, httpClient()),
        vedtakKlient = vedtakKlient,
    )
    val oppgaveService: OppgaveService = OppgaveService(behandlingKlient, vedtakKlient)
    val vedtakService = VedtakService(rapid)
    val grunnlagService = GrunnlagService(behandlingKlient, rapid)

    private fun httpClient() = HttpClient {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        install(Logging) {
            level = LogLevel.HEADERS
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
