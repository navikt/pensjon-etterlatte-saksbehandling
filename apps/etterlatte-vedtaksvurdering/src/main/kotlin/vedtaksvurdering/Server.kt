package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.document
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.security.token.support.v2.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

fun Application.module(vedtaksvurderingService: VedtaksvurderingService, localDev: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(CallLogging) {
        level = Level.INFO
        val naisEndepunkt = listOf("isalive", "isready", "metrics")
        filter { call -> call.request.document().let { !naisEndepunkt.contains(it) } }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
        }
    }

    if (localDev) {
        routingWithouthTokenValidation(vedtaksvurderingService)
    } else {
        routingWithTokenValidation(vedtaksvurderingService)
    }
}
fun Application.routingWithouthTokenValidation(vedtaksvurderingService: VedtaksvurderingService) {
    routing {
        route("api") {
            vilkaarsvurderingRoute(vedtaksvurderingService)
        }
    }
}
fun Application.routingWithTokenValidation(vedtaksvurderingService: VedtaksvurderingService) {
    install(Authentication) {
        tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
    }
    routing {
        route("api") {
            authenticate {
                vilkaarsvurderingRoute(vedtaksvurderingService)
            }
        }
    }
}