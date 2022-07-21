package no.nav.etterlatte.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tilbakekreving(tilbakekrevingService: TilbakekrevingService) {
    route("tilbakekreving") {
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
