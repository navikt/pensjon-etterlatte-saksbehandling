package no.nav.etterlatte.utbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.utbetaling.simulering.SimuleringOsService

internal fun Route.utbetalingRoutes(
    service: SimuleringOsService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = routeLogger

    route("/api/utbetaling") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            post("/simulering") {
                withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                    logger.info("Foretar simulering mot Oppdrag for behandling=$behandlingId")
                    val beregning = service.simuler(behandlingId, brukerTokenInfo).simulering
                    call.respond(beregning ?: HttpStatusCode.NoContent)
                }
            }
        }
    }
}
