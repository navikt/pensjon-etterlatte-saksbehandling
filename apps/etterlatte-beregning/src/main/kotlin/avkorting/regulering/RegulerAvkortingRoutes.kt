package no.nav.etterlatte.avkorting.regulering

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.avkorting.toDto
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker

fun Route.regulerAvkorting(regulerAvkortingService: RegulerAvkortingService, behandlingKlient: BehandlingKlient) {
    val logger = application.log

    route("/api/beregning/avkorting/{$BEHANDLINGSID_CALL_PARAMETER}") {
        post("/med/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Regulere avkorting for behandlingId=$it")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                val avkorting = regulerAvkortingService.regulerAvkorting(it, forrigeBehandlingId, bruker)
                call.respond(avkorting.toDto())
            }
        }
    }
}