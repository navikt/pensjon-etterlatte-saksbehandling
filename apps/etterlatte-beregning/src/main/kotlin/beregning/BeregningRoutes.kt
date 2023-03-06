package no.nav.etterlatte.beregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker

fun Route.beregning(beregningService: BeregningService) {
    route("/api/beregning") {
        val logger = application.log

        get("/{behandlingId}") {
            withBehandlingId {
                logger.info("Henter beregning med behandlingId=$it")
                val beregning = beregningService.hentBeregning(it)
                when (beregning) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(beregning.toDTO())
                }
            }
        }

        post("/{behandlingId}") {
            withBehandlingId {
                logger.info("Oppretter beregning for behandlingId=$it")
                val beregning = beregningService.opprettBeregning(it, bruker)
                call.respond(beregning.toDTO())
            }
        }
    }
}