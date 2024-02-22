package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.application.ServerReady
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.config.ApplicationContext
import no.nav.etterlatte.vedtaksvurdering.klagevedtakRoute
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtakdsvurdering")
    }

    private val engine =
        with(context) {
            embeddedServer(
                factory = CIO,
                environment =
                    applicationEngineEnvironment {
                        config = HoconApplicationConfig(context.config)
                        module {
                            restModule(sikkerlogger(), withMetrics = true) {
                                vedtaksvurderingRoute(
                                    vedtaksvurderingService,
                                    vedtakBehandlingService,
                                    vedtaksvurderingRapidService,
                                    behandlingKlient,
                                )
                                automatiskBehandlingRoutes(automatiskBehandlingService, behandlingKlient)
                                samordningsvedtakRoute(vedtakSamordningService)
                                tilbakekrevingvedtakRoute(vedtakTilbakekrevingService, behandlingKlient)
                                klagevedtakRoute(vedtakKlageService, behandlingKlient)
                            }
                        }
                        module { moduleOnServerReady(context) }
                        connector { port = context.httpPort }
                    },
            )
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady()
            engine.start(true)
        }
}

internal fun Application.moduleOnServerReady(context: ApplicationContext) {
    environment.monitor.subscribe(ServerReady) {
        context.metrikkerJob.schedule().also { addShutdownHook(it) }
    }
}
