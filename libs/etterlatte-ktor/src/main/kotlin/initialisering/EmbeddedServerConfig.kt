package initialisering

import com.typesafe.config.Config
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
): CIOApplicationEngine {
    return embeddedServer(
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                config = HoconApplicationConfig(applicationConfig)
                module {
                    restModule(sikkerlogger(), withMetrics = withMetrics) {
                        routes()
                    }
                }
                connector { port = httpPort }
            },
    )
}

fun initEmbeddedServerUtenRest(
    httpPort: Int,
    applicationConfig: Config,
): CIOApplicationEngine {
    return embeddedServer(
        factory = CIO,
        environment =
            applicationEngineEnvironment {
                config = HoconApplicationConfig(applicationConfig)
                module {
                    routing {
                        healthApi()
                    }
                    metricsModule()
                }
                connector { port = httpPort }
            },
    )
}
