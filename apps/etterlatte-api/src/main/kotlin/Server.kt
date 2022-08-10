package no.nav.etterlatte

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.behandling.grunnlagRoute
import no.nav.etterlatte.behandling.vedtakRoute
import no.nav.etterlatte.health.healthApi
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

class Server(applicationContext: ApplicationContext) {
    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(CallLogging) {
                level = Level.INFO
                filter { call -> !call.request.path().startsWith("/health") }
                format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
                mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
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
                    route("api") {
                        behandlingRoute(applicationContext.behandlingService)
                        oppgaveRoute(applicationContext.oppgaveService)
                        vedtakRoute(applicationContext.vedtakService)
                        grunnlagRoute(applicationContext.grunnlagService)
                    }
                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

