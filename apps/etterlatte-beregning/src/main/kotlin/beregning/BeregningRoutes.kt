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
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.beregning(
    beregningService: BeregningService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning") {
        val logger = application.log

        get("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter beregning med behandlingId=$it")
                val beregning = beregningService.hentBeregning(it)
                when (beregning) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(beregning.toDTO())
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter beregning for behandlingId=$it")
                val beregning = beregningService.opprettBeregning(it, brukerTokenInfo)
                call.respond(beregning.toDTO())
            }
        }

        post("/opprettForOpphoer/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                beregningService.opprettForOpphoer(it, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
