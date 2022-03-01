package no.nav.etterlatte

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.health.healthApi
import no.nav.etterlatte.ktortokenexchange.installAuthUsing
import no.nav.etterlatte.ktortokenexchange.secureRoutUsing
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.person.personApi
import java.util.*

class Server(applicationContext: ApplicationContext) {
    private val personService = applicationContext.personService
    private val securityContext = applicationContext.securityMediator

    private val engine = embeddedServer(CIO, environment = applicationEngineEnvironment {
        module {
            install(ContentNegotiation) { jackson{ objectMapper } }
            installAuthUsing(securityContext)
            install(CallLogging) {
                mdc(CORRELATION_ID) { call -> call.request.header(X_CORRELATION_ID) ?: UUID.randomUUID().toString() }
                filter { call -> !call.request.path().startsWith("/internal") }
            }

            routing {
                healthApi()
                secureRoutUsing(securityContext){
                    personApi(personService)

                }
            }
        }
        connector { port = 8080 }
    })

    fun run() = engine.start(true)
}

