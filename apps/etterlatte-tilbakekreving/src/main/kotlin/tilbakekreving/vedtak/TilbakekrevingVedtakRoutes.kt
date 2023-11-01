package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak

fun Route.tilbakekrevingVedtakRoutes(tilbakekrevingVedtakService: TilbakekrevingVedtakService) {
    val logger = application.log

    route("/api/tilbakekreving/tilbakekrevingsvedtak") {
        post {
            kunSystembruker {
                logger.info("Sender tilbakekrevingsvedtak")
                val vedtak = call.receive<TilbakekrevingVedtak>()
                tilbakekrevingVedtakService.sendTilbakekrevingsvedtak(vedtak)
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
