package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.testRoute () {
    route("test") {
        get {
            call.respond("Test")
        }
    }
}