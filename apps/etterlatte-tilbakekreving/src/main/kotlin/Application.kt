package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.log
import io.ktor.server.routing.application
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.kravgrunnlagRoutes
import no.nav.etterlatte.tilbakekreving.tilbakekrevingRoutes

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(
    private val context: ApplicationContext,
) {
    val devMode = context.properties.devMode

    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-tilbakekreving")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = context.properties.httpPort,
            applicationConfig = ConfigFactory.load(),
        ) {
            tilbakekrevingRoutes(context.tilbakekrevingService)

            if (devMode) {
                if (appIsInGCP()) throw Exception("devMode skal ikke være aktivert i gcp")
                application.log.warn("Legger på endepunkter for kravgrunnlag (kun tilgjengelig lokalt)")
                kravgrunnlagRoutes(context.kravgrunnlagService)
            }
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            if (devMode) {
                if (appIsInGCP()) throw Exception("devMode skal ikke være aktivert i gcp")
            } else {
                kravgrunnlagConsumer.start()
            }

            engine.start(true)
        }
}
