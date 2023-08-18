package no.nav.etterlatte.samordning.vedtak

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.vedtakRoute() {
    route("vedtak") {
        val logger = application.log

        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            call.respond("OK - $vedtakId")
        }
    }
}