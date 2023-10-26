package no.nav.etterlatte.behandling.statistikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

internal fun Route.statistikkRoutes(behandlingService: BehandlingService) {
    val logger = application.log

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
