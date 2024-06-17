package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import org.slf4j.LoggerFactory

fun main() {
    Server(ApplicationContext()).run()
}

class Server(
    private val context: ApplicationContext,
) {
    private val engine =
        initEmbeddedServerUtenRest(
            httpPort = context.httpPort,
            applicationConfig = ConfigFactory.load(),
        )

    fun run() {
        startLytting(context.konsument, LoggerFactory.getLogger(Application::class.java))
        setReady().also { engine.start(true) }
    }
}
