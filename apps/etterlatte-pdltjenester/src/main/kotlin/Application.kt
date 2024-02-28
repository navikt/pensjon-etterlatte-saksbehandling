package no.nav.etterlatte

import io.ktor.server.routing.route
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.person.personRoute
import no.nav.etterlatte.person.personWebRoute
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    Server(ApplicationContext(System.getenv())).run()
}

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-pdltjenester")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = applicationContext.httpPort,
            applicationConfig = applicationContext.config,
        ) {
            personRoute(applicationContext.personService)

            route("/api/pdltjenester") {
                personWebRoute(applicationContext.personService)
            }
        }

    fun run() = setReady().also { engine.start(true) }
}
