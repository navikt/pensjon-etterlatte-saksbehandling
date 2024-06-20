package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.testKravgrunnlagRoutes
import no.nav.etterlatte.tilbakekreving.tilbakekrevingRoutes

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-tilbakekreving")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = context.properties.httpPort,
            applicationConfig = ConfigFactory.load(),
            withMetrics = false,
        ) {
            testKravgrunnlagRoutes(context.kravgrunnlagService)
            tilbakekrevingRoutes(context.tilbakekrevingService)
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            kravgrunnlagConsumer.start()

            setReady()
            engine.start(true)
        }
}
