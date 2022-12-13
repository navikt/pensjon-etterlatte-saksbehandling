package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(context: ApplicationContext) {
    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            modules.add {
                restModule {
                    vilkaarsvurdering(context.vilkaarsvurderingService)
                }
            }
            connector { port = 8080 }
        }
    )

    fun run() {
        engine.start(true)
    }
}