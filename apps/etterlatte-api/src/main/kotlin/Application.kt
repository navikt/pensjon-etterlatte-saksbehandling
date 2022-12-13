package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BeregningKlientImpl
import no.nav.etterlatte.behandling.GrunnlagKlient
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.OppgaveService
import no.nav.etterlatte.behandling.PdltjenesterKlient
import no.nav.etterlatte.behandling.VedtakKlient
import no.nav.etterlatte.behandling.VilkaarsvurderingKlientImpl
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
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val beregningKlient = BeregningKlientImpl(config, httpClient())
    private val vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient())
    private val rapid: KafkaProdusent<String, String> =
        GcpKafkaConfig.fromEnv().standardProducer(System.getenv().getValue("KAFKA_RAPID_TOPIC"))

    val behandlingService: BehandlingService = BehandlingService(
        behandlingKlient = behandlingKlient,
        pdlKlient = PdltjenesterKlient(config, httpClient()),
        vedtakKlient = vedtakKlient,
        grunnlagKlient = grunnlagKlient,
        beregningKlient = beregningKlient,
        vilkaarsvurderingKlient = vilkaarsvurderingKlient
    )
    val oppgaveService: OppgaveService = OppgaveService(behandlingKlient)
    val grunnlagService = GrunnlagService(behandlingKlient, rapid)

    private fun httpClient() = HttpClient {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
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