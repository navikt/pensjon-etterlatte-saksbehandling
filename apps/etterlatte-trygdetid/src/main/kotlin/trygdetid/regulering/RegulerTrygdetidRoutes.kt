package no.nav.etterlatte.trygdetid.regulering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient

fun Route.regulerTrygdetid(regulerTrygdetidService: RegulerTrygdetidService, behandlingKlient: BehandlingKlient) {
    route("/api/trygdetid/{$BEHANDLINGSID_CALL_PARAMETER}/reguler/{forrigeBehandlingId}") {
        val logger = application.log

        post {
            withBehandlingId(behandlingKlient) {
                logger.info("Regulerer trygdetid for behandling $behandlingsId")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                regulerTrygdetidService.regulerTrygdetid(behandlingsId, forrigeBehandlingId, bruker)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}