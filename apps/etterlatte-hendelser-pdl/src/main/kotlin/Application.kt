package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.config.ApplicationContext
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.initialisering.run
import org.slf4j.LoggerFactory

fun main() = Server(ApplicationContext(Miljoevariabler.systemEnv())).run()

class Server(
    private val context: ApplicationContext,
) {
    private val engine = initEmbeddedServerUtenRest(context.httpPort, ConfigFactory.load())

    fun run() {
        lesHendelserFraLeesah(context.leesahKonsument)
        engine.run()
    }
}

fun lesHendelserFraLeesah(leesahKonsument: PersonhendelseKonsument) {
    startLytting(leesahKonsument, LoggerFactory.getLogger(Application::class.java))
}
