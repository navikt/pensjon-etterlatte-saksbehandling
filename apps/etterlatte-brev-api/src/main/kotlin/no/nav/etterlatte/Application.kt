package no.nav.etterlatte

import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.notat.notatRoute
import no.nav.etterlatte.brev.oversendelsebrev.oversendelseBrevRoute
import no.nav.etterlatte.brev.varselbrev.varselbrevRoute
import no.nav.etterlatte.brev.vedtaksbrev.vedtaksbrevRoute
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run

fun main() {
    Server(ApplicationContext()).run()
}

private class Server(
    val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-brev-api") // TODO: naisappame?
    }

    val engine =
        initEmbeddedServer(
            routePrefix = "/api",
            httpPort = context.httpPort,
            applicationConfig = context.config,
        ) {
            brevRoute(
                context.brevService,
                context.pdfService,
                context.brevdistribuerer,
                context.tilgangssjekker,
                context.grunnlagService,
                context.behandlingService,
            )
            vedtaksbrevRoute(context.vedtaksbrevService, context.journalfoerBrevService, context.tilgangssjekker)
            dokumentRoute(context.safService, context.dokarkivService, context.tilgangssjekker)
            varselbrevRoute(context.varselbrevService, context.tilgangssjekker)
            notatRoute(context.notatService, context.nyNotatService, context.tilgangssjekker)
            oversendelseBrevRoute(context.oversendelseBrevService, context.tilgangssjekker)
        }

    fun run() =
        with(context) {
            datasource.migrate()
            engine.run()
        }
}
