package no.nav.etterlatte.samordning

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.vedtak.vedtakRoute
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    Server(ApplicationContext(System.getenv())).run()
}

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-samordning-vedtak oppstart")
    }

    private val engine = embeddedServer(
        CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(applicationContext.config)
            module {
                restModule(sikkerLogg, withMetrics = true) {
                    vedtakRoute()
                }
            }
            connector { port = applicationContext.httpPort }
        }
    )

    fun run() = setReady().also { engine.start(true) }
}