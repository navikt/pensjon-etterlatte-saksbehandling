package no.nav.etterlatte.beregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

fun Route.beregning(
    beregningService: BeregningService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning") {
        val logger = LoggerFactory.getLogger("BeregningRoute")

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter beregning med behandlingId=$it")
                val beregning =
                    beregningService.hentBeregning(it, brukerTokenInfo)
                        ?: throw GenerellIkkeFunnetException()
                call.respond(beregning.toDTO())
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/overstyrt") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter overstyrBeregning med behandlingId=$it")

                val overstyrBeregning = beregningService.hentOverstyrBeregningPaaBehandlingId(behandlingId, brukerTokenInfo)?.toDTO()

                when (overstyrBeregning) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(overstyrBeregning)
                }
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/overstyrt") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Oppretter overstyrBeregning med behandlingId=$it")

                val overstyrBeregning =
                    beregningService
                        .opprettOverstyrBeregning(
                            behandlingId,
                            call.receive<OverstyrBeregningDTO>(),
                            brukerTokenInfo,
                        )!!
                        .toDTO()

                call.respond(overstyrBeregning)
            }
        }

        delete("/{$BEHANDLINGID_CALL_PARAMETER}/overstyrt") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                beregningService.deaktiverOverstyrtberegning(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Oppretter beregning for behandlingId=$it")
                val beregning = beregningService.opprettBeregning(it, brukerTokenInfo)
                call.respond(beregning.toDTO())
            }
        }
    }
}
