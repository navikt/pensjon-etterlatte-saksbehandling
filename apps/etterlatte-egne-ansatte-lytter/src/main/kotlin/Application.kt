package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.etterlatte.kafka.KafkaConsumerEgneAnsatte
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    Server().run()
}

class Server {
    private val engine = embeddedServer(
        factory = io.ktor.server.cio.CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load())
            module {
                routing {
                    healthApi()
                }
            }
            connector { port = 8080 }
        }
    )
    fun run() {
        val env = System.getenv().toMutableMap()
        startEgenAnsattLytter(env, ConfigFactory.load())
        setReady().also { engine.start(true) }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun startEgenAnsattLytter(env: Map<String, String>, config: Config) {
    val logger = LoggerFactory.getLogger(Application::class.java)

    val behandlingHttpClient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("behandling.azure.scope")
    )
    val behandlingKlient = BehandlingKlient(behandlingHttpClient = behandlingHttpClient)
    withLogContext {
        GlobalScope.launch {
            try {
                KafkaConsumerEgneAnsatte(env = env, behandlingKlient = behandlingKlient).poll()
            } catch (e: Exception) {
                logger.error("App avsluttet med en feil", e)
                exitProcess(1)
            }
        }
    }
}