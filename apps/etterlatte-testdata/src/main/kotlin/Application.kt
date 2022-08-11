package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testdata.features.SlettsakFeature
import testdata.features.egendefinert.EgendefinertMeldingFeature
import testdata.features.index.IndexFeature
import testdata.features.standardmelding.StandardMeldingFeature

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val logger: Logger = LoggerFactory.getLogger("BEY001")
val producer = GcpKafkaConfig.fromEnv(System.getenv()).standardProducer(System.getenv().getValue("KAFKA_TARGET_TOPIC"))
interface TestDataFeature{
    val beskrivelse: String
    val path: String
    val routes: Route.()->Unit
}
val features: List<TestDataFeature> = listOf(
    IndexFeature,
    EgendefinertMeldingFeature,
    StandardMeldingFeature,
    SlettsakFeature,
)

fun main() {
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

                    features.forEach{
                        route(it.path){
                            apply(it.routes)
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



fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()