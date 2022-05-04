package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("{behandlingId}") {
            val behandlingId = context.parameters["behandlingId"]!!

            call.respond(service.hentBrev(behandlingId))
        }

        get("{vedtakId}/send") {
            call.respond("OK")
        }
    }
}
