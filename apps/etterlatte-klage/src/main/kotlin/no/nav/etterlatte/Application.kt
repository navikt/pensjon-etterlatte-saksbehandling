package no.nav.etterlatte

import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.klage.ApplicationContext
import no.nav.etterlatte.klage.kabalOvesendelseRoute
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory

fun main() {
    Server(ApplicationContext()).run()
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-klage")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = context.httpPort,
            applicationConfig = context.config,
        ) {
            with(context) {
                kabalOvesendelseRoute(
                    kabalOversendelseService = kabalOversendelseService,
                )
            }
        }

    fun run() {
        startLytting(konsument = context.kabalKafkakonsument, logger = LoggerFactory.getLogger(Application::class.java))
        setReady().also { engine.start(wait = true) }
    }
}
