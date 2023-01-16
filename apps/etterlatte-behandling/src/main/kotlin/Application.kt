package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
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
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.database.DatabaseContext
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.sak.sakRoutes
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
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

fun Application.sikkerhetsModul() {
    install(Authentication) {
        tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
    }
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

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/internal") }
        format { call -> "<- ${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}" }
        mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("En feil oppstod: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, "En feil oppstod: ${cause.message}")
        }
    }

    val generellBehandlingService = beanFactory.generellBehandlingService()
    val grunnlagsendringshendelseService = beanFactory.grunnlagsendringshendelseService()

    routing {
        naisprobes()
        authenticate {
            attachContekst(ds.dataSource)
            sakRoutes(beanFactory.sakService(), generellBehandlingService, grunnlagsendringshendelseService)
            behandlingRoutes(
                generellBehandlingService,
                beanFactory.foerstegangsbehandlingService(),
                beanFactory.revurderingService(),
                beanFactory.manueltOpphoerService()
            )
            behandlingsstatusRoutes(beanFactory.foerstegangsbehandlingService())
            route("api") {
                oppgaveRoutes(OppgaveDao(ds.dataSource))
            }
            grunnlagsendringshendelseRoute(grunnlagsendringshendelseService)
        }
    }
    beanFactory.behandlingHendelser().start()
}

private fun Route.attachContekst(ds: DataSource) {
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst =
            Context(
                decideUser(call.principal<TokenValidationContextPrincipal>()!!),
                DatabaseContext(ds)
            )
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
        beanFactory.grunnlagsendringshendelseJob()
        embeddedServer(
            CIO,
            applicationEngineEnvironment {
                modules.add { module(beanFactory) }
                modules.add { sikkerhetsModul() }
                connector { port = 8080 }
            }
        ).start(true)
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