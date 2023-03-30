package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.healthApi() {
    route("health") {
        get("isalive") {
            call.respond(HttpStatusCode.OK)
        }

        get("isready") {
            call.respond(isReady())
        }
    }
}