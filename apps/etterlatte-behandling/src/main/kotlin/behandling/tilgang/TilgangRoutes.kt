package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.TILGANG_ROUTE_PATH
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse

internal fun Route.tilgangRoutes(sakService: SakService, sakServiceAdressebeskyttelse: SakServiceAdressebeskyttelse) {
    route("/$TILGANG_ROUTE_PATH") {
        post("/person") {
            val fnr = call.receive<String>()
            val harTilgang = !sakService.sjekkOmFnrPaaSakHarAdresseBeskyttelse(fnr)
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            val harTilgang =
                !sakServiceAdressebeskyttelse.behandlingHarAdressebeskyttelse(behandlingsId.toString())
            call.respond(harTilgang)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val harTilgang = !sakService.sjekkOmSakHarAdresseBeskyttelse(sakId)
            call.respond(harTilgang)
        }
    }
}