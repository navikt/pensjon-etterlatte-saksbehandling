package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.sak.sakRoutes
import org.slf4j.event.Level
import java.util.*
import javax.sql.DataSource

fun main() {
    ventPaaNettverk()
    appFromEnv(System.getenv()).run()
}

fun appFromEnv(env: Map<String, String>): App {
    return appFromBeanfactory(EnvBasedBeanFactory(env))
}

fun appFromBeanfactory(env: BeanFactory): App {
    return App(env)
}

fun Application.module(beanFactory: BeanFactory) {
    val ds = beanFactory.datasourceBuilder().apply {
        migrate()
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(Authentication) {
        beanFactory.tokenValidering()()
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

    routing {
        naisprobes()
        authenticate {
            attachContekst(ds.dataSource)
            sakRoutes(beanFactory.sakService())
            behandlingRoutes(
                beanFactory.generellBehandlingService(),
                beanFactory.foerstegangsbehandlingService(),
                beanFactory.revurderingService()
            )
            oppgaveRoutes(OppgaveDao(ds.dataSource))
        }

    }
    beanFactory.behandlingHendelser().start()
}

private fun Route.attachContekst(ds: DataSource) {
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst = Context(decideUser(call.principal()!!), DatabaseContext(ds))
        withContext(
            Dispatchers.Default + Kontekst.asContextElement(
                value = requestContekst
            )
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}

class App(private val beanFactory: BeanFactory) {
    fun run() {
        embeddedServer(CIO, applicationEngineEnvironment {
            modules.add { module(beanFactory) }
            connector { port = 8080 }
        }).start(true)
        beanFactory.behandlingHendelser().nyHendelse.close()
    }
}

private fun ventPaaNettverk() {
    runBlocking { delay(5000) }
}

fun Route.naisprobes() {
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
