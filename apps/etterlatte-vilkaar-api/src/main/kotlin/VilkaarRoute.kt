package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.VilkaarRoute(service: VilkaarService) {
    route("vilkaarresultat") {
        val logger = application.log

        get("/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]
            logger.info("Henter vurdert vilkaarsresultat for behandlingsId $behandlingId")

            behandlingId?.let {
                service.hentVilkaarResultat(behandlingId)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound)
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
}