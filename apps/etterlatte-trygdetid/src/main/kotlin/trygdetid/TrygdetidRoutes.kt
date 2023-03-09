package no.nav.etterlatte.trygdetid

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.trygdetid() {
    route("/api/trygdetid") {
        val logger = application.log

        get {
            logger.info("Henter trygdetid")
            call.respond("Suh dude")
        }
    }
}