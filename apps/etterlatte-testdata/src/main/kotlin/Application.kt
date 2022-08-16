package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mustachejava.DefaultMustacheFactory
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.mustache.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import testdata.features.SlettsakFeature
import testdata.features.egendefinert.EgendefinertMeldingFeature
import testdata.features.index.IndexFeature
import testdata.features.soeknad.OpprettSoeknadFeature
import testdata.features.standardmelding.StandardMeldingFeature
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

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
    OpprettSoeknadFeature(ConfigFactory.load(), httpClient()),
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

private fun httpClient() = HttpClient {
    install(ClientContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    defaultRequest {
        header(X_CORRELATION_ID, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()

fun getAccessToken(call: ApplicationCall): String {
    val authHeader = call.request.parseAuthorizationHeader()
    if (!(authHeader == null
                || authHeader !is HttpAuthHeader.Single
                || authHeader.authScheme != "Bearer")
    ) {
        return authHeader.blob
    }
    throw Exception("Missing authorization header")
}
