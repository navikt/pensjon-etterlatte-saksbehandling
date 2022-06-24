package no.nav.etterlatte.tilbakekreving

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext
import java.util.*

fun Route.tilbakekreving(tilbakekrevingService: TilbakekrevingService) {
    get("/tilbakekreving/{kravgrunnlagid}") {
        call.respond(
            tilbakekrevingService.hentTilbakekreving(kravgrunnlagId) ?: HttpStatusCode.NotFound
        )
    }

    /* TODO: endepunkt for å hente tilbakekreving basert på behandlingId (UUID eller UUID30)
    get("/tilbakekreving/{behandlingsid}"){
        call.respond(
            tilbakekrevingService.hentTilbakekreving(behandlingsId) ?: HttpStatusCode.NotFound
        )
    }
     */

}

inline val PipelineContext<*, ApplicationCall>.kravgrunnlagId get() = requireNotNull(call.parameters[""]).toLong()

// TODO: metode for å hente ut UUID eller UUID30 - avhengig av hva vi går for i behandling
inline val PipelineContext<*, ApplicationCall>.behandlingsId
    get() = requireNotNull(call.parameters["behandlingsid"]).let {
        UUID.fromString(
            it
        )
    }
