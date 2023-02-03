package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.beregning.beregning
import no.nav.etterlatte.beregning.config.ApplicationContext
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-beregning oppstart")
    }

    private val engine = with(context) {
        embeddedServer(
            factory = CIO,
            environment = applicationEngineEnvironment {
                module {
                    restModule(sikkerLogg) {
                        beregning(beregningService)
                    }
                }
                connector { port = properties.httpPort }
            }
        )
    }

    fun run() = with(context) {
        dataSource.migrate()
        engine.start(true)
    }
}