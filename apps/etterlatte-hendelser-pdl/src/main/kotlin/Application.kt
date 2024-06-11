package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.config.ApplicationContext
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory

fun main() {
    val context = ApplicationContext(System.getenv())
    Server(context).run()
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
        lesHendelserFraLeesah(context.leesahKonsument)
        setReady().also { engine.start(true) }
    }
}

fun lesHendelserFraLeesah(leesahKonsument: PersonhendelseKonsument) {
    startLytting(leesahKonsument, LoggerFactory.getLogger(Application::class.java))
}
