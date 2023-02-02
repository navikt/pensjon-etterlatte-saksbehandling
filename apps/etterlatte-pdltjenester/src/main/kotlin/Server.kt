package no.nav.etterlatte

import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
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
import no.nav.etterlatte.ktortokenexchange.SecurityContextMediator
import no.nav.etterlatte.ktortokenexchange.installAuthUsing
import no.nav.etterlatte.ktortokenexchange.secureRouteUsing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.person.PdlFantIkkePerson
import no.nav.etterlatte.person.PersonService
import no.nav.etterlatte.person.personApi
import org.slf4j.event.Level
import setReady
import java.util.*

class Server(applicationContext: ApplicationContext) {

    private val engine = embeddedServer(
        CIO,
        environment = applicationEngineEnvironment {
            module {
                module(
                    securityContextMediator = applicationContext.securityMediator,
                    personService = applicationContext.personService
                )
            }
            connector { port = 8080 }
        }
    )

    fun run() = engine.start(true).also { setReady() }
}

fun io.ktor.server.application.Application.module(
    securityContextMediator: SecurityContextMediator,
    personService: PersonService
) {
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
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(
                TextContent(
                    "En feil oppstod: ${cause.message}",
                    ContentType.Text.Plain,
                    HttpStatusCode.InternalServerError
                )
            )
        }
        exception<PdlFantIkkePerson> { call, cause ->
            call.application.log.info("Fant ikke person: ${cause.message}")
            call.respond(
                TextContent(
                    "Fant ikke person",
                    ContentType.Text.Plain,
                    HttpStatusCode.NotFound
                )
            )
        }
    }
    installAuthUsing(securityContextMediator)

    routing {
        healthApi()
        secureRouteUsing(securityContextMediator) {
            personApi(personService)
        }
    }
}