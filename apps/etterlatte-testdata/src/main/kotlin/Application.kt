package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.get
import com.github.mustachejava.DefaultMustacheFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.mustache.Mustache
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.logging.NAV_CONSUMER_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.firstValidTokenClaims
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.testdata.dolly.DollyClientImpl
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.features.SlettsakFeature
import no.nav.etterlatte.testdata.features.dolly.DollyFeature
import no.nav.etterlatte.testdata.features.egendefinert.EgendefinertMeldingFeature
import no.nav.etterlatte.testdata.features.index.IndexFeature
import no.nav.etterlatte.testdata.features.soeknad.OpprettSoeknadFeature
import no.nav.etterlatte.testdata.features.standardmelding.StandardMeldingFeature
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val env = System.getenv()

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val logger: Logger = LoggerFactory.getLogger("testdata")
val localDevelopment = env["DEV"].toBoolean()
val httpClient = httpClient(forventSuksess = true)
val config: Config = ConfigFactory.load()
val azureAdClient = AzureAdClient(config, httpClient)

val producer = if (localDevelopment) {
    LocalKafkaConfig(env["KAFKA_BROKERS"]!!).standardProducer(env["KAFKA_TARGET_TOPIC"]!!)
} else {
    GcpKafkaConfig.fromEnv(System.getenv()).standardProducer(System.getenv().getValue("KAFKA_TARGET_TOPIC"))
}

interface TestDataFeature {
    val beskrivelse: String
    val path: String
    val routes: Route.() -> Unit
}

val features: List<TestDataFeature> = listOf(
    IndexFeature,
    EgendefinertMeldingFeature,
    StandardMeldingFeature,
    SlettsakFeature,
    OpprettSoeknadFeature,
    DollyFeature(DollyService(DollyClientImpl(config, httpClient)))
)

fun main() {
    embeddedServer(
        CIO,
        applicationEngineEnvironment {
            module {
                install(Mustache) {
                    mustacheFactory = DefaultMustacheFactory("templates")
                }
                install(CallLogging) {
                    level = org.slf4j.event.Level.INFO
                    filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics")) }
                }
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.application.log.error("En feil oppstod: ${cause.message}", cause)
                        call.respond(HttpStatusCode.InternalServerError, "En intern feil har oppstått")
                    }
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
                    metricsModule()
                }
            }
            connector { port = 8080 }
        }
    ).start(true)
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

fun httpClient() = httpClient(
    forventSuksess = true,
    ekstraDefaultHeaders = {
        it.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        it.header(NAV_CONSUMER_ID, "etterlatte-testdata")
    }
)

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() =
    call.firstValidTokenClaims()?.get("NAVident")?.toString()

fun PipelineContext<Unit, ApplicationCall>.usernameFraToken() =
    call.firstValidTokenClaims()?.get("preferred_username")?.toString()

fun getClientAccessToken(): String = runBlocking {
    azureAdClient.getAccessTokenForResource(listOf("api://${config.getString("dolly.client.id")}/.default"))
        .get()!!.accessToken
}