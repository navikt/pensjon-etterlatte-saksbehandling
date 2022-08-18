package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.testRoute() {
    route("test") {
        get {
            call.respond("Test")
        }
    }
}