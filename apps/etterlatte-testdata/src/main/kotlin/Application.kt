package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mustachejava.DefaultMustacheFactory
import com.typesafe.config.ConfigFactory
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.mustache.Mustache
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testdata.features.SlettsakFeature
import testdata.features.egendefinert.EgendefinertMeldingFeature
import testdata.features.index.IndexFeature
import testdata.features.soeknad.OpprettSoeknadFeature
import testdata.features.standardmelding.StandardMeldingFeature

private val env = System.getenv()

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val logger: Logger = LoggerFactory.getLogger("BEY001")
val localDevelopment = env["DEV"].toBoolean()

val producer = if (localDevelopment) {
    LocalKafkaConfig(env["KAFKA_BROKERS"]!!).standardProducer(env["KAFKA_TARGET_TOPIC"]!!)
} else {
    GcpKafkaConfig.fromEnv(System.getenv()).standardProducer(System.getenv().getValue("KAFKA_TARGET_TOPIC"))
}

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
    OpprettSoeknadFeature,
)

fun main() {
    embeddedServer(CIO, applicationEngineEnvironment {
        module {
            install(Mustache) {
                mustacheFactory = DefaultMustacheFactory("templates")
            }

            if (localDevelopment) {
                routing {
                    api()
                }
            } else {
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }

                routing {
                    get("/isalive") { call.respondText("ALIVE", ContentType.Text.Plain) }
                    get("/isready") { call.respondText("READY", ContentType.Text.Plain) }

                    authenticate {
                        api()
                    }
                }
            }
        }
        connector { port = 8080 }
    }).start(true)
}

private fun Route.api() {
    features.forEach {
        route(it.path) {
            apply(it.routes)
        }
    }

    route("/kafka") {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        post {
            producer.publiser("0", objectMapper.writeValueAsString(call.receive<ObjectNode>()))
            call.respondText("Record lagt på kafka", ContentType.Text.Plain)
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()