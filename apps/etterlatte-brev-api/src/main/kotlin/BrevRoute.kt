package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("opprett") {
            call.respond(service.opprettBrev(1))
        }
    }
}
