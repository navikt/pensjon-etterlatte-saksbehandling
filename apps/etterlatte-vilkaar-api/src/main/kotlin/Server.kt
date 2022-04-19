package no.nav.etterlatte

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.health.healthApi
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.event.Level
import java.util.*

class Server(applicationContext: ApplicationContext) {

    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            module(applicationContext)
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

fun Application.module(applicationContext: ApplicationContext) {
    val vilkaarDao = applicationContext.vilkaarDao()
    val vilkaarService = applicationContext.vilkaarService(vilkaarDao)
    val tokenValidering = applicationContext.tokenValidering()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/internal") }
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
        tokenValidering(this)
    }

    routing {
        healthApi()
        authenticate {
            VilkaarRoute(vilkaarService)
        }
    }
}

