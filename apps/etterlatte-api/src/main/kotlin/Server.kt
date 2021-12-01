package no.nav.etterlatte

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.health.healthApi

class Server(applicationContext: ApplicationContext) {
    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) { jackson() }

            install(CallLogging) {
                filter { call -> !call.request.path().startsWith("/internal") }
            }

            routing {
                healthApi()
                testRoute()
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

