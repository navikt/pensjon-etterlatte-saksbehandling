package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.hendelserpdl.leesah.KafkaConsumerHendelserPdl
import no.nav.etterlatte.hendelserpdl.leesah.LivsHendelserTilRapid
import no.nav.etterlatte.hendelserpdl.leesah.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main() {
    Server().run()
}

class Server() {
    private val engine = embeddedServer(
        factory = CIO,
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
        startLeesahKonsumer(env)
        setReady().also { engine.start(true) }
    }
}

fun startLeesahKonsumer(env: Map<String, String>) {
    val logger = LoggerFactory.getLogger(Application::class.java)
    val pdlTjenester: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("PDL_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) }
        )
    }
    val pdlService by lazy {
        PdlService(pdlTjenester, "http://etterlatte-pdltjenester")
    }
    val topic = env.getValue("KAFKA_RAPID_TOPIC")
    // Gcpkafkaconfig burde renames?
    val kafkaProducer = GcpKafkaConfig.fromEnv(env).rapidsAndRiversProducer(topic)
    val closed = AtomicBoolean()
    closed.set(false)

    Thread {
        withLogContext {
            try {
                val kafkaConsumerHendelserPdl = KafkaConsumerHendelserPdl(
                    PersonHendelseFordeler(LivsHendelserTilRapid(kafkaProducer), pdlService),
                    env,
                    closed
                )
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        closed.set(true)
                        kafkaConsumerHendelserPdl.getConsumer().wakeup(); // trådsikker
                    }
                )

                kafkaConsumerHendelserPdl.stream()
            } catch (e: Exception) {
                logger.error("App avsluttet med en feil", e)
                exitProcess(1)
            }
        }
    }.start()
}