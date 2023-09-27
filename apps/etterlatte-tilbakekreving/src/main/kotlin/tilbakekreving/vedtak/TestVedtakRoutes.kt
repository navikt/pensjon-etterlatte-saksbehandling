package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId

/**
 * Denne brukes kun for å teste at oppsettet via proxyen fungerer. Fjernes senere.
 */
fun Route.testTilbakekrevingsvedtak(tilbakekrevingKlient: TilbakekrevingKlient) {
    route("tilbakekrevingsvedtak") {
        post {
            tilbakekrevingKlient.sendTilbakekrevingsvedtak(Tilbakekrevingsvedtak(VedtakId(1)))
            call.respond("OK")
        }
    }
}
