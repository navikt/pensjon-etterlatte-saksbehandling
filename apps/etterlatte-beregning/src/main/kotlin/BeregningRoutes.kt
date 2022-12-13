package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.getAccessToken

fun Route.beregning(beregningService: BeregningService) {
    route("/api/beregning") {
        get("/{behandlingId}") {
            withBehandlingId {
                val beregning = beregningService.hentBeregning(it)
                when (beregning) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(beregning.toDTO())
                }
            }
        }

        post("/{behandlingId}") {
            withBehandlingId {
                val accessToken = getAccessToken(call)
                val beregning = beregningService.lagreBeregning(it, accessToken)
                call.respond(beregning.toDTO())
            }
        }
    }
}