package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.getAccessToken
import java.util.*

fun Route.beregning(beregningService: BeregningService) {
    route("/api/beregning") {
        get("/{beregningsid}") {
            withBehandlingId {
                val beregning = beregningService.hentBeregning(it)
                call.respond(beregning)
            }
        }

        post("/{behandlingsid}/opprett") {
            withBehandlingId {
                val accessToken = getAccessToken(call)
                val beregning = beregningService.lagreBeregning(it, accessToken)
                call.respond(beregning)
            }
        }
    }
}

private suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) {
    val id = call.parameters["behandlingId"]
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Fant ikke behandlingId")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, "behandlingId må være en UUID")
    }
}