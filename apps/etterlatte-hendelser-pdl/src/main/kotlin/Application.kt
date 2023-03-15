package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.etterlatte.hendelserpdl.leesah.KafkaConsumerHendelserPdl
import no.nav.etterlatte.hendelserpdl.leesah.LivsHendelserTilRapid
import no.nav.etterlatte.hendelserpdl.leesah.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.security.ktor.clientCredential
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    Server().run()
}

class Server {
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
        startLeesahLytter(env)
        setReady().also { engine.start(true) }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun startLeesahLytter(env: Map<String, String>) {
    val logger = LoggerFactory.getLogger(Application::class.java)
    val pdlService by lazy {
        PdlService(pdlHttpClient(env), "http://etterlatte-pdltjenester")
    }
    val topic = env.getValue("KAFKA_RAPID_TOPIC")
    // Gcpkafkaconfig burde renames?
    val kafkaProducer = GcpKafkaConfig.fromEnv(env).rapidsAndRiversProducer(topic)
    withLogContext {
        GlobalScope.launch {
            try {
                val kafkaConsumerHendelserPdl = KafkaConsumerHendelserPdl(
                    PersonHendelseFordeler(LivsHendelserTilRapid(kafkaProducer), pdlService),
                    env
                )
                while (true) {
                    kafkaConsumerHendelserPdl.stream()
                }
            } catch (e: Exception) {
                logger.error("App avsluttet med en feil", e)
                exitProcess(1)
            }
        }
    }
}

fun pdlHttpClient(props: Map<String, String>) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(Auth) {
        clientCredential {
            config = props.toMutableMap()
                .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
        }
    }
    defaultRequest {
        header(HttpHeaders.XCorrelationId, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }