package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.config.ApplicationProperties

fun main() {
    val applicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val context = ApplicationContext(applicationProperties)
    Server(context).run()
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-tilbakekreving")
    }

    private val engine =
        embeddedServer(
            factory = CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(ConfigFactory.load())
                    module {
                        routing { healthApi() }
                        metricsModule()
                    }
                    connector { port = 8080 }
                },
        )

    fun run() =
        with(context) {
            // kravgrunnlagConsumer.start() TODO - må få økonomi til å gjøre oppsett for kø

            setReady()
            engine.start(true)
        }
}
