package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.config.ApplicationContext
import no.nav.etterlatte.vedtaksvurdering.etteroppgjoerSystembrukerVedtakRoute
import no.nav.etterlatte.vedtaksvurdering.klagevedtakRoute
import no.nav.etterlatte.vedtaksvurdering.samordningSystembrukerVedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute

fun main() {
    ApplicationContext().let { Server(it).runServer() }
}

class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstart("etterlatte-vedtakdsvurdering")
    }

    private val engine =
        with(context) {
            initEmbeddedServer(
                httpPort = context.httpPort,
                applicationConfig = context.config,
                cronJobs =
                    listOf(
                        context.metrikkerJob,
                        context.outboxJob,
                    ),
            ) {
                vedtaksvurderingRoute(
                    vedtaksvurderingService,
                    vedtakBehandlingService,
                    vedtaksvurderingRapidService,
                    behandlingKlient,
                )
                automatiskBehandlingRoutes(automatiskBehandlingService, behandlingKlient)
                samordningSystembrukerVedtakRoute(vedtakSamordningService)
                etteroppgjoerSystembrukerVedtakRoute(vedtakEtteroppgjoerService, behandlingKlient)
                tilbakekrevingvedtakRoute(vedtakTilbakekrevingService, behandlingKlient)
                klagevedtakRoute(vedtakKlageService, behandlingKlient)
            }
        }

    fun runServer() =
        with(context) {
            dataSource.migrate()
            engine.runEngine()
        }
}
