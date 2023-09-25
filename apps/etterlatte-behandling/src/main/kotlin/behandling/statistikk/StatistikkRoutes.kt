package no.nav.etterlatte.behandling.statistikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.BehandlingForStatistikk
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

internal fun Route.statistikkRoutes(statistikkService: StatistikkService) {
    val logger = application.log

    route("/behandlinger/statistikk") {
        route("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            get {
                logger.info("Henter detaljert behandling for behandling med id=$behandlingsId")
                when (val behandling = statistikkService.hentDetaljertBehandling(behandlingsId, brukerTokenInfo)) {
                    is BehandlingForStatistikk -> call.respond(behandling)
                    else -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandling med id=$behandlingsId")
                }
            }
        }
    }
}
