package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.klage.ApplicationContext
import no.nav.etterlatte.klage.module
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory

fun main() {
    Server(ApplicationContext()).run()
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-klage")
    }

    private val engine =
        embeddedServer(
            factory = CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(context.config)
                    module { module(context) }
                    connector { port = context.httpPort }
                },
        )

    fun run() {
        startLytting(konsument = context.kabalKafkakonsument, logger = LoggerFactory.getLogger(Application::class.java))
        setReady().also { engine.start(wait = true) }
    }
}
