package no.nav.etterlatte

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.testRoute () {
    route("test") {
        get {
            call.respond("Test")
        }
    }
}