package no.nav.etterlatte.behandling

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.getAccessToken

fun Route.vedtakRoute(service: VedtakService) {

    route("vedtak") {
        post("{behandlingId}"){
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Behandlings-id mangler")
            } else {
                call.respond(service.fattVedtak(behandlingId, getAccessToken(call)))
            }
        }
    }

}