package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.config.ApplicationContext
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    val context = ApplicationContext(System.getenv())
    Server(context).run()
}

class Server(private val context: ApplicationContext) {
    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load())
            module {
                routing {
                    healthApi()
                }
                metricsModule()
            }
            connector { port = context.httpPort }
        }
    )

    fun run() {
        lesHendelserFraLeesah(context.leesahKonsument)
        setReady().also { engine.start(true) }
    }
}

fun lesHendelserFraLeesah(leesahKonsument: PersonhendelseKonsument) {
    val logger = LoggerFactory.getLogger(Application::class.java)

    thread(start = true) {
        withLogContext {
            try {
                leesahKonsument.stream()
            } catch (e: Exception) {
                logger.error("etterlatte-hendelser-pdl avsluttet med en feil", e)
                exitProcess(1)
            }
        }
    }
}