package no.nav.etterlatte

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level

fun Application.apiModule(routes: Route.() -> Unit) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/internal") }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(no.nav.etterlatte.libs.common.logging.CORRELATION_ID) { call ->
            call.request.header(no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID) ?: java.util.UUID.randomUUID()
                .toString()
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, "En intern feil har oppstått")
        }
    }

    install(Authentication) {
        tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
    }

    routing {
        healthApi()
        authenticate {
            route("api") {
                routes()
            }
        }
    }
}

fun Route.healthApi() {
    route("internal") {
        get("isalive") {
            call.respondText { "OK" }
        }
        get("isready") {
            call.respondText { "OK" }
        }
        get("started") {
            call.respondText { "OK" }
        }
    }
}
