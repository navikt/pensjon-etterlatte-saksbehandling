package no.nav.etterlatte.behandling.vedtaksbehandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.kunSystembruker

internal fun Route.vedtaksbehandlingRoutes(vedtaksbehandlingService: VedtaksbehandlingService) {
    get("/vedtaksbehandling/{$BEHANDLINGID_CALL_PARAMETER}/redigerbar") {
        kunSystembruker {
            val redigerbar =
                inTransaction {
                    vedtaksbehandlingService.erBehandlingRedigerbar(behandlingId)
                }
            call.respond(HttpStatusCode.OK, redigerbar)
        }
    }
}
