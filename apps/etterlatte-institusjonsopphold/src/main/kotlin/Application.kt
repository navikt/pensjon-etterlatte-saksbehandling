package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.kafka.KafkaConsumerInstitusjonsopphold
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main() {
    Server().run()
}

class Server {
    private val defaultConfig: Config = ConfigFactory.load()
    private val engine = embeddedServer(
        factory = io.ktor.server.cio.CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(defaultConfig)
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
        startEgenAnsattLytter(env, defaultConfig)
        setReady().also { engine.start(true) }
    }
}

fun startEgenAnsattLytter(env: Map<String, String>, config: Config) {
    val logger = LoggerFactory.getLogger(Application::class.java)

    val behandlingHttpClient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("behandling.azure.scope")
    )
    val behandlingKlient = BehandlingKlient(behandlingHttpClient = behandlingHttpClient)

    val closed = AtomicBoolean()
    closed.set(false)

    withLogContext {
        Thread {
            try {
                val kafkaConsumerInstitusjonsopphold = KafkaConsumerInstitusjonsopphold(
                    env = env,
                    closed = closed,
                    behandlingKlient = behandlingKlient
                )

                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        closed.set(true)
                        kafkaConsumerInstitusjonsopphold.getConsumer().wakeup()
                        // trådsikker, aborterer konsumer fra polling
                    }
                )

                kafkaConsumerInstitusjonsopphold.stream()
            } catch (e: Exception) {
                logger.error("App avsluttet med en feil", e)
                exitProcess(1)
            }
        }.start()
    }
}