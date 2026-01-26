package no.nav.etterlatte

import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import org.slf4j.LoggerFactory

fun main() {
    Server(ApplicationContext()).runServer()
}

class Server(
    private val applicationContext: ApplicationContext,
) {
    private val logger = LoggerFactory.getLogger(Application::class.java)

    private val engine =
        initEmbeddedServerUtenRest(
            httpPort = 8080,
            applicationConfig = applicationContext.defaultConfig,
        )

    fun runServer() {
        startLytting(
            konsument = applicationContext.kafkaConsumerInstitusjonsopphold,
            logger = logger,
        )
        engine.runEngine()
    }
}
