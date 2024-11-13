package no.nav.etterlatte.behandling.statistikk

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

internal fun Route.statistikkRoutes(behandlingService: BehandlingService) {
    val logger = LoggerFactory.getLogger("StatistikkRoute")

    route("/behandlinger/statistikk/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            logger.info("Henter detaljert behandling for behandling med id=$behandlingId")
            val behandling =
                behandlingService.hentStatistikkBehandling(behandlingId, brukerTokenInfo)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(behandling)
        }
    }
}
