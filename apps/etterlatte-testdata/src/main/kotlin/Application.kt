package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.get
import com.github.mustachejava.DefaultMustacheFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
import io.ktor.server.http.content.staticResources
import io.ktor.server.mustache.Mustache
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.callloging.processingTimeMillis
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.ktor.X_USER
import no.nav.etterlatte.libs.ktor.firstValidTokenClaims
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdHttpClient
import no.nav.etterlatte.libs.ktor.metricsRoute
import no.nav.etterlatte.libs.ktor.skjulAllePotensielleFnr
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.no.nav.etterlatte.testdata.features.OpprettOgBehandle
import no.nav.etterlatte.no.nav.etterlatte.testdata.features.automatisk.Familieoppretter
import no.nav.etterlatte.testdata.dolly.DollyClientImpl
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.dolly.TestnavClient
import no.nav.etterlatte.testdata.features.dolly.DollyFeature
import no.nav.etterlatte.testdata.features.egendefinert.EgendefinertMeldingFeature
import no.nav.etterlatte.testdata.features.index.IndexFeature
import no.nav.etterlatte.testdata.features.samordning.SamordningMottattFeature
import no.nav.etterlatte.testdata.features.soeknad.OpprettSoeknadFeature
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val env = System.getenv()

val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val logger: Logger = LoggerFactory.getLogger("testdata")
val localDevelopment = env["DEV"].toBoolean()
val httpClient = httpClient(forventSuksess = true)
val config: Config = ConfigFactory.load()
val azureAdClient = AzureAdClient(config, AzureAdHttpClient(httpClient))

val producer =
    if (localDevelopment) {
        LocalKafkaConfig(env["KAFKA_BROKERS"]!!).standardProducer(env["KAFKA_TARGET_TOPIC"]!!)
    } else {
        GcpKafkaConfig.fromEnv(System.getenv()).standardProducer(System.getenv().getValue("KAFKA_TARGET_TOPIC"))
    }

interface TestDataFeature {
    val beskrivelse: String
    val path: String
    val routes: Route.() -> Unit
}

val dollyService =
    DollyService(
        DollyClientImpl(config, httpClient),
        TestnavClient(config, httpClient),
    )
val features: List<TestDataFeature> =
    listOf(
        IndexFeature,
        EgendefinertMeldingFeature,
        OpprettSoeknadFeature,
        DollyFeature(dollyService),
        OpprettOgBehandle(dollyService, Familieoppretter(dollyService)),
        SamordningMottattFeature,
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
                    filter { call -> !call.request.path().matches(Regex(".*/isready|.*/isalive|.*/metrics|.*/static")) }

                    format { call ->
                        val responseTime = call.processingTimeMillis()
                        val status = call.response.status()?.value
                        val method = call.request.httpMethod.value
                        val path = call.request.path()

                        skjulAllePotensielleFnr(
                            "<- $status $method $path in $responseTime ms",
                        )
                    }

                    mdc(X_USER) { call ->
                        call.request.header("Authorization")?.let {
                            val token = JwtToken(it.substringAfterLast("Bearer "))
                            val jwtTokenClaims = token.jwtTokenClaims
                            jwtTokenClaims.get("NAVident") as? String // human
                                ?: token.jwtTokenClaims.get("azp_name") as? String // system/app-user
                        }
                    }
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

                        staticResources("/static", "static")

                        authenticate {
                            api()
                        }
                    }
                    metricsRoute()
                }
            }
            connector { port = 8080 }
        },
    ).start(true)
}

private fun Route.api() {
    features.forEach {
        route(it.path) {
            apply(it.routes)
        }
    }
}

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.firstValidTokenClaims()?.get("NAVident")?.toString()

fun PipelineContext<Unit, ApplicationCall>.brukerIdFraToken() = call.firstValidTokenClaims()?.get("oid")?.toString()

fun getDollyAccessToken(): String =
    runBlocking {
        azureAdClient.hentTokenFraAD(
            Systembruker.testdata,
            listOf("api://${config.getString("dolly.client.id")}/.default"),
        )
            .get()!!.accessToken
    }

fun getTestnavAccessToken(): String =
    runBlocking {
        azureAdClient.hentTokenFraAD(
            Systembruker.testdata,
            listOf("api://${config.getString("testnav.client.id")}/.default"),
        ).get()!!.accessToken
    }
