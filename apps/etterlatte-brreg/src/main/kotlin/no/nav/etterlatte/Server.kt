package no.nav.etterlatte

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                exception<Throwable> { call, cause ->
                    call.application.log.error("En feil oppstod: ${cause.message}", cause)
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
