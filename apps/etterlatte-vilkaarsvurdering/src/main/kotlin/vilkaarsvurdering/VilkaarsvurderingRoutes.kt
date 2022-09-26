package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.vilkaarsvurdering() {
    route("vilkaarsvurdering") {
        val logger = application.log

        get("/") {
            call.respond("test" ?: HttpStatusCode.NotFound)
        }
    }
}