package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import no.nav.etterlatte.behandling.BehandlingService

fun Route.behandlingRoute (service: BehandlingService) {
    route("api/personer/{fnr}") {
        get {
            val fnr = call.parameters["fnr"]
            if(fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {

                val authHeader = call.request.parseAuthorizationHeader()

                val accessToken = when {
                    authHeader is HttpAuthHeader.Single && authHeader.authScheme.lowercase() in listOf("bearer") -> authHeader.authScheme.lowercase()
                    else -> null
                } ?: throw Error("Access-token mangler")

                println(authHeader)

                val list = service.hentPerson(fnr, accessToken)
                call.respond(list)


            }
        }
    }
}