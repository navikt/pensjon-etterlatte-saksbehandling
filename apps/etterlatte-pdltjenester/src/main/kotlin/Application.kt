package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.person.personRoute
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    Server(ApplicationContext()).run()
}

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-pdltjenester oppstart")
    }

    private val engine = embeddedServer(
        CIO,
        environment = applicationEngineEnvironment {
            module {
                restModule(sikkerLogg) {
                    personRoute(applicationContext.personService)
                }
            }
            connector { port = 8080 }
        }
    )

    fun run() = setReady().also { engine.start(true) }
}