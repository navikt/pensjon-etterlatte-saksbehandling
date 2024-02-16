package no.nav.etterlatte.behandling.generiskbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId

internal fun Route.generiskBehandlingRoutes(generiskBehandlingService: GeneriskBehandlingService) {
    get("/generiskbehandling/{$BEHANDLINGID_CALL_PARAMETER}/redigerbar") {
        val redigerbar = generiskBehandlingService.erBehandlingRedigerbar(behandlingId)
        call.respond(HttpStatusCode.OK, redigerbar)
    }
}
