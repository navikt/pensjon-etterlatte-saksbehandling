package no.nav.etterlatte.vilkaar

import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.get
import io.ktor.routing.route
import no.nav.etterlatte.getAccessToken

fun Route.vilkaarRoute(service: VilkaarService) {

    route("vurdertvilkaar") {
        val logger = application.log

        get("/{behandlingId}") {
            val accessToken = getAccessToken(call)
            val behandlingId = call.parameters["behandlingId"]
            logger.info("Henter vurdert vilkaar for behandoingId $behandlingId")

            behandlingId?.let {
                service.hentVurdertVilkaar(behandlingId, accessToken).let {
                    call.respond(it)
                }
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
}