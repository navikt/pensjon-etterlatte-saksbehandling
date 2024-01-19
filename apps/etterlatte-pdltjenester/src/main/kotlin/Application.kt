package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.person.personRoute
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
        embeddedServer(
            CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(applicationContext.config)
                    module {
                        restModule(sikkerLogg, withMetrics = true) {
                            personRoute(applicationContext.personService)
                        }
                    }
                    connector { port = applicationContext.httpPort }
                },
        )

    fun run() = setReady().also { engine.start(true) }
}
