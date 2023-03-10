package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse

internal fun Route.tilgangRoutes(sakService: SakService, sakServiceAdressebeskyttelse: SakServiceAdressebeskyttelse) {
    route("/tilgang") {
        post("/person") {
            val fnr = call.receive<String>()
            call.respond(sakService.sjekkOmSakHarStrengtFortroligBeskyttelse(fnr))
        }

        get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            call.respond(sakServiceAdressebeskyttelse.behandlingHarAdressebeskyttelse(behandlingsId.toString()))
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val sakId = call.parameters[SAKID_CALL_PARAMETER]!!.toLong()
            call.respond(sakService.sjekkOmSakHarStrengtFortroligBeskyttelse(sakId))
        }
    }
}