package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import no.nav.etterlatte.health.healthApi
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.medl.medlemsregisterApi
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level
import setReady
import java.util.*

class Server(context: ApplicationContext) {
    private val engine = embeddedServer(
        CIO,
        environment = applicationEngineEnvironment {
            module {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }

                install(CallLogging) {
                    level = Level.INFO
                    filter { call -> !call.request.path().startsWith("/health") }
                    format { call ->
                        "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}"
                    }
                    mdc(CORRELATION_ID) { call ->
                        call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString()
                    }
                }

                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.application.log.error("En feil oppstod: ${cause.message}", cause)
                        call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
                    }
                }

                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }

                routing {
                    healthApi()
                    authenticate {
                        medlemsregisterApi(context.medlService)
                    }
                }
            }
            connector { port = 8080 }
        }
    )

    fun run() = engine.start(true).also { setReady() }
}