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
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.batch.payload
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val aremark_person = "12101376212"
val logger: Logger = LoggerFactory.getLogger("BEY001")

fun main() {

    val env = System.getenv()
    val topic = env.getValue("KAFKA_TARGET_TOPIC")
    logger.info("Konfig lest, oppretter kafka-produsent")

    val producer = GcpKafkaConfig.fromEnv(env).standardProducer(topic)



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
                    get("/") {
                        call.respondHtml {
                            this.head {
                                title { +"Post melding til Kafka" }
                            }
                            body {
                                h2 {
                                    +"Post meldinger til Kafka"
                                }
                                p {
                                    +"Innlogget som ${navIdentFraToken() ?: "Anonym"}"
                                }
                                h4 {
                                    +"Meny"
                                }
                                ul {
                                    li {
                                        a {
                                            href = "/postmelding"
                                            +"Post egendefinert melding"
                                        }
                                    }
                                    li {
                                        a {
                                            href = "/sendMelding"
                                            +"Post standardmelding"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    get("/sendMelding") {
                        sendMelding(
                            payload(aremark_person),
                            producer
                        )

                        call.respondHtml {
                            this.head {
                                title { +"Post melding til Kafka" }
                            }
                            body {
                                h3 {
                                    +"Standardmelding postet!"
                                }
                                br {}
                                ul {
                                    li {
                                        a {
                                            href = "/"
                                            +"Tilbake til hovedmeny"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    get("/postmelding") {
                        call.respondHtml {
                            this.head {
                                title { +"Post melding til Kafka" }
                            }
                            body {
                                form(action = "/postmelding", method = FormMethod.post) {
                                    label {
                                        htmlFor = "key"
                                        +"Nøkkel:"
                                    }
                                    br { }
                                    textInput {
                                        name = "key"
                                        id = "key"
                                    }
                                    br { }
                                    label {
                                        htmlFor = "json"
                                        +"Melding:"
                                    }
                                    br { }
                                    textArea {
                                        name = "json"
                                        id = "json"
                                    }
                                    br { }
                                    submitInput()
                                }
                                ul {
                                    li {
                                        a {
                                            href = "/"
                                            +"Tilbake til hovedmeny"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    post("/postmelding") {

                        val offset = call.receiveParameters().let {
                            producer.publiser(
                                requireNotNull(it["key"]),
                                JsonMessage(requireNotNull(it["json"])).toJson(),
                                mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                            )
                        }
                        logger.info("Publiserer melding med partisjon: ${offset.first} offset: ${offset.second}")

                        call.respondHtml {
                            this.head {
                                title { +"Post melding til Kafka" }
                            }
                            body {
                                h3 {
                                    +"Melding postet!"
                                }
                                p { +"Partisjon: ${offset.first} Offset: ${offset.second}" }
                                br {}
                                ul {
                                    li {
                                        a {
                                            href = "/postmelding"
                                            +"Post ny melding"
                                        }
                                    }
                                    li {
                                        a {
                                            href = "/"
                                            +"Tilbake til hovedmeny"
                                        }
                                    }
                                }
                            }
                        }

                    }


                    post("/kafka") {
                        producer.publiser("0", objectMapper.writeValueAsString(call.receive<ObjectNode>()))
                        call.respondText("Record lagt på kafka", ContentType.Text.Plain)
                    }

                }
            }
        }
        connector { port = 8080 }
    }).start(true)
}

internal fun sendMelding(
    melding: String,
    producer: KafkaProdusent<String, String>,
) {
    val startMillis = System.currentTimeMillis()
    logger.info("Publiserer melding")

    createRecord(melding).also { producer.publiser(it.first, it.second) }

    logger.info("melding publisert på ${(System.currentTimeMillis() - startMillis) / 1000}s ")
}

private fun createRecord(input: String): Pair<String, String> {
    val message = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "soeknad_innsendt",
            "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
            "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
            "@template" to "soeknad",
            "@fnr_soeker" to aremark_person,
            "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString()
        )
    )
    return "0" to message.toJson()
}

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()