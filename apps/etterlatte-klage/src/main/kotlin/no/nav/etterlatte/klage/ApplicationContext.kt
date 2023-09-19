package no.nav.etterlatte.klage

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory

internal class ApplicationContext {

    private val defaultConfig: Config = ConfigFactory.load()
    private val env = System.getenv()
    private val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(defaultConfig)
            module {
                routing {
                    healthApi()
                }
                metricsModule()
            }
            connector { port = httpPort }
        }
    )

    fun run() {
        startKafkalytter(defaultConfig, env)
        setReady().also { engine.start(true) }
    }
}

private fun startKafkalytter(config: Config, env: Map<String, String>) {
    val behandlingHttpClient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("behandling.azure.scope")
    )

    startLytting(
        konsument = KlageKafkakonsument(
            env = env,
            topic = env.requireEnvValue("KLAGE_TOPIC"),
            behandlingKlient = BehandlingKlient(config, behandlingHttpClient = behandlingHttpClient)
        ),
        logger = LoggerFactory.getLogger(Application::class.java)
    )
}