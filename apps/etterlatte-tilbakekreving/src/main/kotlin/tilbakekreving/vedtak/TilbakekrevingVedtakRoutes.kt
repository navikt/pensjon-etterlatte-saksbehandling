package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.routeLogger

fun Route.tilbakekrevingVedtakRoutes(tilbakekrevingVedtakService: TilbakekrevingVedtakService) {
    val logger = routeLogger

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
