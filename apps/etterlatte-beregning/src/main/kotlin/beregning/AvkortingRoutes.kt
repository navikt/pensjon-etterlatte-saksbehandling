package no.nav.etterlatte.beregning

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId

fun Route.avkorting(avkortingService: AvkortingService, behandlingKlient: BehandlingKlient) {
    route("/api/avkorting") {
        val logger = application.log

        get("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                val avkorting = avkortingService.hentAvkorting(it)
                call.respond(avkorting)
            }
        }
    }
}