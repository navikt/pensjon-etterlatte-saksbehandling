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
import no.nav.etterlatte.ktortokenexchange.installAuthUsing
import no.nav.etterlatte.ktortokenexchange.secureRoutUsing
import no.nav.etterlatte.person.personApi

class Server(applicationContext: ApplicationContext) {
    //private val personService = applicationContext.personService
    private val personServiceAad = applicationContext.personServiceAad
    private val securityContext = applicationContext.securityMediator

    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) { jackson() }
            installAuthUsing(securityContext)

            install(CallLogging) {
                filter { call -> !call.request.path().startsWith("/internal") }
            }

            routing {
                healthApi()
                secureRoutUsing(securityContext){
                    personApi(personServiceAad)

                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

