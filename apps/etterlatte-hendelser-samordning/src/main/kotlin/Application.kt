import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import org.slf4j.LoggerFactory

fun main() {
    Server(ApplicationContext()).run()
}

class Server(private val context: ApplicationContext) {
    private val engine =
        embeddedServer(
            factory = CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(ConfigFactory.load())
                    module {
                        routing {
                            healthApi()
                        }
                        metricsModule()
                    }
                    connector { port = context.httpPort }
                },
        )

    fun run() {
        startLytting(context.konsument, LoggerFactory.getLogger(Application::class.java))
        setReady().also { engine.start(true) }
    }
}
