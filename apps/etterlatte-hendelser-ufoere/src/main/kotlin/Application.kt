package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.hendelserufoere.config.ApplicationContext
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import org.slf4j.LoggerFactory

fun main() = Server(ApplicationContext(Miljoevariabler.systemEnv())).runServer()

class Server(
    private val context: ApplicationContext,
) {
    private val engine = initEmbeddedServerUtenRest(context.httpPort, ConfigFactory.load())

    fun runServer() {
        lesHendelserFraUfoere(context.ufoereKonsument)
        engine.runEngine()
    }
}

fun lesHendelserFraUfoere(ufoereKonsument: UfoerehendelseKonsument) {
    startLytting(ufoereKonsument, LoggerFactory.getLogger(Application::class.java))
}
