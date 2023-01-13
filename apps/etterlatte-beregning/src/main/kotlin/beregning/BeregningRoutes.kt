package no.nav.etterlatte.beregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.accesstoken

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
                val beregning = beregningService.lagreBeregning(it, accesstoken)
                call.respond(beregning.toDTO())
            }
        }
    }
}