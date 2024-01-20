package no.nav.etterlatte.libs.ktor.initialisering

import com.typesafe.config.Config
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.healthApi
import no.nav.etterlatte.libs.ktor.metricsModule
import no.nav.etterlatte.libs.ktor.restModule

fun initEmbeddedServer(
    httpPort: Int,
    applicationConfig: Config,
    withMetrics: Boolean = true,
    routes: Route.() -> Unit,
) = settOppEmbeddedServer(httpPort, applicationConfig) {
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
    metricsModule()
}

private fun settOppEmbeddedServer(
    httpPort: Int,
    applicationConfig: Config,
    body: Application.() -> Unit,
): CIOApplicationEngine =
    embeddedServer(
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                config = HoconApplicationConfig(applicationConfig)
                module {
                    body()
                }
                connector { port = httpPort }
            },
    )
