package no.nav.etterlatte.tilbakekreving

import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.get
import io.ktor.routing.route

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
