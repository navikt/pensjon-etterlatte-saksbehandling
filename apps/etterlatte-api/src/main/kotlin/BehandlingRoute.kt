package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import no.nav.etterlatte.behandling.BehandlingService

fun Route.behandlingRoute (service: BehandlingService) {
    route("/api/personer/{fnr}") {
        get {
            val fnr = call.parameters["fnr"]
            if(fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                service.hentPerson(fnr)
            }
        }
    }
}