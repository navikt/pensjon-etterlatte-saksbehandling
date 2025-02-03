package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.ktor.healthApi

fun main() {
    // sikre is_healthy / is_alive
    embeddedServer(
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                module {
                    routing {
                        healthApi()
                    }
                }
                connector { port = 8080 }
            },
    ).start(wait = true)
}
