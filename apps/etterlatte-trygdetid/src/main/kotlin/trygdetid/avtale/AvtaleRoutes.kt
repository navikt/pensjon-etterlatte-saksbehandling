package no.nav.etterlatte.trygdetid.avtale

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.avtale(avtaleService: AvtaleService) {
    route("/api/trygdetid/avtaler") {
        val logger = application.log

        get {
            logger.info("Henter alle avtaler")
            call.respond(avtaleService.hentAvtaler())
        }

        get("/kriteria") {
            logger.info("Henter alle avtalekriterier")
            call.respond(avtaleService.hentAvtaleKriterier())
        }
    }
}