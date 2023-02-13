package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.ktortokenexchange.installAuthUsing
import no.nav.etterlatte.ktortokenexchange.secureRouteUsing
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.person.personApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

class Server(applicationContext: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-vilkaarsvurdering oppstart")
    }

    private val engine = embeddedServer(
        CIO,
        environment = applicationEngineEnvironment {
            module { module(applicationContext) }
            connector { port = 8080 }
        }
    )

    fun run() = setReady().also { engine.start(true) }
}

fun Application.module(applicationContext: ApplicationContext) {
    restModule(sikkerLogg) {
        secureRouteUsing(applicationContext.securityMediator) {
            personApi(applicationContext.personService)
        }
    }.apply {
        installAuthUsing(applicationContext.securityMediator)
    }
}