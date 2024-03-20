package no.nav.etterlatte.libs.ktor.initialisering

import com.typesafe.config.Config
import io.ktor.server.application.Application
import io.ktor.server.application.ServerReady
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.etterlatte.addShutdownHook
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.ktor.shutdownPolicyEmbeddedServer
import no.nav.etterlatte.libs.ktor.metricsRoute
import no.nav.etterlatte.libs.ktor.restModule
import java.util.Timer

fun initEmbeddedServer(
    httpPort: Int,
    applicationConfig: Config,
    withMetrics: Boolean = true,
    shutdownHooks: List<Timer> = listOf(),
    routes: Route.() -> Unit,
) = settOppEmbeddedServer(httpPort, applicationConfig, shutdownHooks) {
    restModule(sikkerlogger(), withMetrics = withMetrics) {
        routes()
    }
}

fun initEmbeddedServerUtenRest(
    httpPort: Int,
    applicationConfig: Config,
) = settOppEmbeddedServer(httpPort, applicationConfig) {
    routing {
        healthApi()
    }
    metricsRoute()
}

private fun settOppEmbeddedServer(
    httpPort: Int,
    applicationConfig: Config,
    shutdownHooks: List<Timer> = listOf(),
    body: Application.() -> Unit,
): CIOApplicationEngine =
    embeddedServer(
        configure = shutdownPolicyEmbeddedServer(),
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                config = HoconApplicationConfig(applicationConfig)
                module {
                    body()
                }
                module {
                    environment.monitor.subscribe(ServerReady) {
                        shutdownHooks.forEach { addShutdownHook(it) }
                    }
                }
                connector { port = httpPort }
            },
    )
