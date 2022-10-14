package no.nav.etterlatte.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.tilbakekreving(tilbakekrevingService: TilbakekrevingService) {
    route("api/tilbakekreving") {
        val logger = application.log

        get("/{kravgrunnlagid}") {
            val kravgrunnlagId = requireNotNull(call.parameters["kravgrunnlagid"]).toLong()
            logger.info("Henter kravgrunnlag med id=$kravgrunnlagId")
            call.respond(
                tilbakekrevingService.hentTilbakekreving(kravgrunnlagId) ?: HttpStatusCode.NotFound
            )
        }
    }
}