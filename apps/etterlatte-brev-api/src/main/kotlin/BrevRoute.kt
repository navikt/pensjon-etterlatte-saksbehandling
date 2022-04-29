package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("{vedtakId}/opprett") {
            val vedtakId = context.parameters["vedtakId"]

            if (!vedtakId.isNullOrEmpty()) {
                call.respond(service.opprettBrev(vedtakId))
            } else {
               call.respond(HttpStatusCode.BadRequest, "VedtakId mangler")
            }
        }

        get("{vedtakId}/send") {
            call.respond("OK")
        }
    }
}
