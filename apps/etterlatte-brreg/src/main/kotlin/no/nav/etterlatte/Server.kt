package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.enhetsregister.enhetsregApi
import no.nav.etterlatte.health.healthApi
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import org.slf4j.event.Level
import java.util.UUID

class Server(context: ApplicationContext) {
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
                    call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
                }
            }

            // TODO: Trenger vi egentlig auth på denne appen... ?
//            install(Authentication) {
//                tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
//            }

            routing {
                healthApi()
                // TODO: Trenger vi egentlig auth på denne appen... ?
//                authenticate {
                    enhetsregApi(context.service)
//                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}
