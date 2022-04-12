package no.nav.etterlatte


import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.OppgaveService
import no.nav.etterlatte.behandling.PdltjenesterKlient
import no.nav.etterlatte.behandling.VedtakService
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.vilkaar.VilkaarKlient

class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    private val behandlingKlient = BehandlingKlient(config, httpClient())
    val behandlingService: BehandlingService = BehandlingService(
        behandlingKlient = behandlingKlient,
        pdlKlient = PdltjenesterKlient(config, httpClient()),
        vilkaarKlient = VilkaarKlient(config, httpClient())
    )

    val oppgaveService: OppgaveService = OppgaveService(behandlingKlient)

    val vedtakService = VedtakService(behandlingKlient)

    //private val vilkaarKlient = VilkaarKlient(config, httpClient())
    //val vilkaarService = VilkaarService(vilkaarKlient)

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
