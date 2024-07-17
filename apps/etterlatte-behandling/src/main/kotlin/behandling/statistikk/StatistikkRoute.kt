package no.nav.etterlatte.behandling.statistikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

internal fun Route.statistikkRoutes(behandlingService: BehandlingService) {
    val logger = routeLogger

    route("/behandlinger/statistikk/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            logger.info("Henter detaljert behandling for behandling med id=$behandlingId")
            when (val behandling = behandlingService.hentStatistikkBehandling(behandlingId, brukerTokenInfo)) {
                is StatistikkBehandling -> call.respond(behandling)
                else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingId")
            }
        }
    }
}
