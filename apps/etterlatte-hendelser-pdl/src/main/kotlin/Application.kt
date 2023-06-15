package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.hendelserpdl.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.config.ApplicationContext
import no.nav.etterlatte.hendelserpdl.leesah.LeesahConsumer
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {
    val context = ApplicationContext(System.getenv())
    Server(context).run()
}

class Server(val context: ApplicationContext) {

    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load())
            module {
                routing {
                    healthApi()
                }
            }
            connector { port = context.httpPort }
        }
    )

    fun run() {
        lesHendelserFraLeesah(context.leesahConsumer, context.personHendelseFordeler)
        setReady().also { engine.start(true) }
    }
}

fun lesHendelserFraLeesah(leesahConsumer: LeesahConsumer, personHendelseFordeler: PersonHendelseFordeler) {
    val logger = LoggerFactory.getLogger(Application::class.java)

    thread(start = true) {
        withLogContext {
            try {
                val readyToConsume = AtomicBoolean(true)

                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        readyToConsume.set(false)
                        leesahConsumer.consumer.wakeup(); // trådsikker, aborter konsumer fra polling
                    }
                )

                leesahConsumer.lesHendelserFraLeesah(readyToConsume) {
                    personHendelseFordeler.haandterHendelse(it)
                }
            } catch (e: Exception) {
                logger.error("etterlatte-hendelser-pdl avsluttet med en feil", e)
                exitProcess(1)
            }
        }
    }
}