package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.health.healthApi
import no.nav.security.token.support.ktor.tokenValidationSupport

class Server(applicationContext: ApplicationContext) {
    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) { jackson() }
            install(CallLogging) {
                filter { call -> !call.request.path().startsWith("/internal") }
            }

            install(Authentication) {
                tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
            }

            routing {
                healthApi()
                authenticate {
                    route("api") {
                        behandlingRoute(applicationContext.behandlingService)
                    }
                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

