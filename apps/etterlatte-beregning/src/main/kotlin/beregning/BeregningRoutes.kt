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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.beregning(
    beregningService: BeregningService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning") {
        val logger = application.log

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter beregning med behandlingId=$it")
                when (val beregning = beregningService.hentBeregning(it, brukerTokenInfo)) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(beregning.toDTO())
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/overstyrt") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter overstyrBeregning med behandlingId=$it")

                val overstyrBeregning = beregningService.hentOverstyrBeregning(behandlingId, brukerTokenInfo).toDTO()

                when (overstyrBeregning) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(overstyrBeregning)
                }
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter beregning for behandlingId=$it")
                val beregning = beregningService.opprettBeregning(it, brukerTokenInfo)
                call.respond(beregning.toDTO())
            }
        }

        post("/opprettForOpphoer/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                beregningService.opprettForOpphoer(it, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
