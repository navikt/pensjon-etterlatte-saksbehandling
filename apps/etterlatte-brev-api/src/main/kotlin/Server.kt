package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import health.healthApi
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
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

class Server(applicationContext: ApplicationContext) {
    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
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
                format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
                mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
            }

            install(StatusPages) {
                exception<Throwable> { cause ->
                    log.error("En feil oppstod: ${cause.message}", cause)
                    call.respond(HttpStatusCode.InternalServerError, "En intern feil har oppstått")
                }
            }

            if (applicationContext.localDevelopment) {
                routing { brevRoute(applicationContext.brevService) }
            } else {
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }

                routing {
                    healthApi()
                    authenticate {
                        route("api") {
                            brevRoute(applicationContext.brevService)
                        }
                    }
                }
            }
        }
        connector {
            port = ConfigFactory.load().tryGetString("port")?.toInt() ?: 8080
        }
    })

    fun run() = engine.start(true)
}
