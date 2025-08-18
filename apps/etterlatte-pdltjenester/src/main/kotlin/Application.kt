package no.nav.etterlatte

import io.ktor.server.routing.route
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.person.personRoute
import no.nav.etterlatte.personweb.SporingService
import no.nav.etterlatte.personweb.personWebRoute
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    Server(ApplicationContext(Miljoevariabler.systemEnv())).runServer()
}

class Server(
    applicationContext: ApplicationContext,
) {
    init {
        sikkerLoggOppstart("etterlatte-pdltjenester")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = applicationContext.httpPort,
            applicationConfig = applicationContext.config,
        ) {
            personRoute(applicationContext.personService)

            route("/api/pdltjenester") {
                personWebRoute(applicationContext.personWebService, SporingService(Sporingslogg()))
            }
        }

    fun runServer() = engine.runEngine()
}
