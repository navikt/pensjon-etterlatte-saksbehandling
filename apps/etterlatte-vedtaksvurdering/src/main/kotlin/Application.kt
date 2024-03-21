package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.config.ApplicationContext
import no.nav.etterlatte.vedtaksvurdering.klagevedtakRoute
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.tilbakekrevingvedtakRoute
import no.nav.etterlatte.vedtaksvurdering.vedtaksvurderingRoute
import java.time.Duration
import java.util.UUID
import kotlin.concurrent.thread

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtakdsvurdering")
    }

    private val engine =
        with(context) {
            initEmbeddedServer(
                httpPort = context.httpPort,
                applicationConfig = context.config,
                shutdownHooks = listOf(context.metrikkerJob.schedule()),
            ) {
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

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady()
            thread {
                Thread.sleep(Duration.ofMinutes(1))
                behandlinger.forEach { behandling ->
                    logger.info("Lager melding for behandling $behandling")
                    val melding =
                        runBlocking {
                            vedtakBehandlingService.lagMelding(UUID.fromString(behandling), Systembruker.tekniskRetting)
                        }
                    melding?.let {
                        vedtaksvurderingRapidService.sendToRapid(it)
                        logger.info("Sendte ut melding på nytt for behandling $behandling")
                    }
                }
            }
            engine.start(true)
        }
}

val behandlinger =
    listOf(
        "c8849fc0-924d-4350-bb56-1429732445c9",
        "16a0a8c2-6841-4745-9453-f915e8136d24",
        "b18e5209-ab30-458f-81d5-3c761059fac5",
        "5678d7f7-8782-4204-9a5a-38f25c06b1f8",
        "b4896813-dcd2-46ea-b3fd-e66c3e58dbb4",
    )
