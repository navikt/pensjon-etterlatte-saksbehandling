package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.batch.KafkaConfig
import no.nav.etterlatte.batch.payload
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val aremark_person = "12101376212"
val logger:Logger = LoggerFactory.getLogger("BEY001")

fun main() {

    logger.info("Batch startet")
    val env = System.getenv()

    val config = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env.getValue("KAFKA_KEYSTORE_PATH"),
        keystorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD")
    )
    val topic = env.getValue("KAFKA_TARGET_TOPIC")
    logger.info("Konfig lest, oppretter kafka-produsent")

    val producer = KafkaProducer(config.producerConfig(), StringSerializer(), StringSerializer())

    embeddedServer(CIO, applicationEngineEnvironment {
        module {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
            install(Authentication) {
                tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
            }
            routing {
                get("/isalive") { call.respondText("ALIVE", ContentType.Text.Plain) }
                get("/isready") { call.respondText("READY", ContentType.Text.Plain) }
                authenticate {
                    get("/"){
                        call.respondText (navIdentFraToken()?: "Anonym", ContentType.Text.Plain )                    }
                    get("/sendMelding") {
                        sendMelding(
                            payload(aremark_person),
                            producer,
                            topic
                        )
                        call.respondText("READY", ContentType.Text.Plain)
                    }

                    post("/kafka") {
                        producer.send(ProducerRecord(topic, "0", objectMapper.writeValueAsString(call.receive<ObjectNode>())))
                        call.respondText("Record lagt på kafka", ContentType.Text.Plain)
                    }

                }
            }
        }
        connector { port=8080 }
    }).start(true)
}

internal fun sendMelding(
    melding: String,
    producer: Producer<String, String>,
    topic: String
) {
    val startMillis = System.currentTimeMillis()
    logger.info("Publiserer melding")

    producer.send(createRecord(melding, topic)).get()


    producer.flush()
    producer.close()

    logger.info("melding publisert på ${(System.currentTimeMillis() - startMillis) / 1000}s")
}

private fun createRecord(input: String, topic: String): ProducerRecord<String, String> {
    val message = JsonMessage.newMessage(mapOf(
        "@event_name" to "soeknad_innsendt",
        "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
        "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
        "@template" to "soeknad",
        "@fnr_soeker" to aremark_person,
        "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString()
    ))
    return ProducerRecord(topic, "0", message.toJson())
}
fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()