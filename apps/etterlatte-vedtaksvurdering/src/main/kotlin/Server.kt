package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.event.Level
import java.util.*

fun Application.module(vedtaksvurderingService: VedtaksvurderingService) {

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

        install(Authentication) {
            tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
        }

        routing {
            authenticate {
                route("api") {
                    Api(vedtaksvurderingService)
                }
            }
        }
}



/*
class Server(val vedtaksvurderingService: VedtaksvurderingService) {
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

            install(Authentication) {
                tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
            }

            routing {
                naisprobes()
                authenticate {
                    route("api") {
                        Api(vedtaksvurderingService)
                    }
                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

fun Route.naisprobes(){
    route("internal"){
        get("isalive"){
            call.respondText { "OK" }
        }
        get("isready"){
            call.respondText { "OK" }
        }
        get("started"){
            call.respondText { "OK" }
        }
    }
}

 */