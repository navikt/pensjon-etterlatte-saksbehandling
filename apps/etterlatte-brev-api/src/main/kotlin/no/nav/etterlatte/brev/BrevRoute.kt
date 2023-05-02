package no.nav.etterlatte.brev

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        post("{brevId}/pdf") {
            val brevId = call.parameters["brevId"]!!
            val innhold = service.hentBrevInnhold(brevId.toLong())

            call.respond(innhold.data)
        }
    }
}