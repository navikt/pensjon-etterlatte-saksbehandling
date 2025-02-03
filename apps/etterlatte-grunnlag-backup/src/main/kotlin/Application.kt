package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.ktor.shutdownPolicyEmbeddedServer

fun main() {
    // sikre is_healthy / is_alive
    embeddedServer(
        configure = shutdownPolicyEmbeddedServer(),
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                config = HoconApplicationConfig(ConfigFactory.load())
                module {
                    routing {
                        healthApi()
                    }
                }
                connector { port = 8080 }
            },
    )
}
