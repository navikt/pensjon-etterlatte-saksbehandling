package model

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.model.BeregningService

fun Route.beregning(beregningService: BeregningService) {
    route("api/beregning") {
        get("{behandlingid}") {
        }
        post("opprett") {
        }
        post("oppdater") {
        }
    }
}