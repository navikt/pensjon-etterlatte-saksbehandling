package no.nav.etterlatte.migrering

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import migrering.ApplicationContext
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-migrering oppstart")
    }

    private val engine = with(context) {
        embeddedServer(
            factory = CIO,
            environment = applicationEngineEnvironment {
                config = HoconApplicationConfig(context.config)
                module {
                    restModule(sikkerLogg) {
                    }
                }
                connector { port = properties.httpPort }
            }
        )
    }

    fun run() = with(context) {
        dataSource.migrate()
        setReady()
        engine.start(true)
    }
}