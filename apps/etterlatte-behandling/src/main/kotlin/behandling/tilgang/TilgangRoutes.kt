package no.nav.etterlatte.behandling.tilgang

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse

internal fun Route.tilgangRoutes(sakService: SakService, sakServiceAdressebeskyttelse: SakServiceAdressebeskyttelse) {
    val logger = application.log

    route("/tilgang") {
        post("/person") {
            val fnr = call.receive<String>()
            val harTilgang = !sakService.sjekkOmSakHarStrengtFortroligBeskyttelse(fnr)
            logger.info("har tilgang for ${fnr.maskerFnr()} $harTilgang")
            call.respond(harTilgang)
        }

        get("/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            val behandlingHarAdressebeskyttelse =
                sakServiceAdressebeskyttelse.behandlingHarAdressebeskyttelse(behandlingsId.toString())
            logger.info("har tilgang for $behandlingsId $behandlingHarAdressebeskyttelse")
            call.respond(behandlingHarAdressebeskyttelse)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            val sakId = call.parameters[SAKID_CALL_PARAMETER]!!.toLong()
            val harTilgang = !sakService.sjekkOmSakHarStrengtFortroligBeskyttelse(sakId)
            logger.info("har tilgang for $sakId $harTilgang")
            call.respond(harTilgang)
        }
    }
}