package no.nav.etterlatte

import io.ktor.server.plugins.swagger.swaggerUI
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.config.ApplicationContext
import no.nav.etterlatte.vedtaksvurdering.dollybehandling.dollyRoute
import no.nav.etterlatte.vedtaksvurdering.klagevedtakRoute
import no.nav.etterlatte.vedtaksvurdering.samordningSystembrukerVedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute

fun main() {
    ApplicationContext().let { Server(it).run() }
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
                tilbakekrevingvedtakRoute(vedtakTilbakekrevingService, behandlingKlient)
                klagevedtakRoute(vedtakKlageService, behandlingKlient)
                if (!isProd()) {
                    swaggerUI(path = "dolly/swagger", swaggerFile = "vedtaksvurderingSwaggerV1.yaml")
                    dollyRoute(vedtaksvurderingService, dollyService)
                }
            }
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            engine.run()
        }
}
