package no.nav.etterlatte

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import no.nav.etterlatte.samordning.vedtak.samordningVedtakRoute
import no.nav.etterlatte.samordning.vedtak.serverRequestLoggerPlugin
import no.nav.etterlatte.samordning.vedtak.userIdMdcPlugin
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    Server(ApplicationContext(Miljoevariabler(System.getenv()))).run()
}

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-samordning-vedtak oppstart")
    }

    private val engine =
        embeddedServer(
            CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(applicationContext.config)

                    module {
                        restModule(
                            sikkerLogg,
                            withMetrics = true,
                        ) {
                            samordningVedtakRoute(samordningVedtakService = applicationContext.samordningVedtakService)
                            install(userIdMdcPlugin)
                        }
                        install(serverRequestLoggerPlugin)
                    }
                    connector { port = applicationContext.httpPort }
                },
        )

    fun run() = setReady().also { engine.start(true) }
}
