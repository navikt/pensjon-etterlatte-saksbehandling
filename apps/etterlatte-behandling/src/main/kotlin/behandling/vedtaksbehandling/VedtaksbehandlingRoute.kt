package no.nav.etterlatte.behandling.vedtaksbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId

internal fun Route.behandlingMedBrevRoutes(behandlingMedBrevService: BehandlingMedBrevService) {
    get("/behandling-med-brev/{$BEHANDLINGID_CALL_PARAMETER}/redigerbar") {
        val redigerbar =
            inTransaction {
                behandlingMedBrevService.erBehandlingRedigerbar(behandlingId)
            }
        call.respond(HttpStatusCode.OK, redigerbar)
    }
}
