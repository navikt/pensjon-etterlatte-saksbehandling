package no.nav.etterlatte

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.behandling.BehandlingService

fun getAccessToken(call: ApplicationCall): String {
    val authHeader = call.request.parseAuthorizationHeader()
    if (!(authHeader == null
                || authHeader !is HttpAuthHeader.Single
                || authHeader.authScheme != "Bearer")
    ) {
        return authHeader.blob
    }
    throw Exception("Missing authorization header")
}

// /api
fun Route.behandlingRoute(service: BehandlingService) {

    route("saker") {
        //hent alle saker

        get {
            try {
                val accessToken = getAccessToken(call)
                val list = service.hentSaker(accessToken)
                call.respond(list)
            } catch (e: Exception) {
                throw e
            }
        }

        // hent spesifikk sak (alle behandlinger?)
        get("/{sakId}") {
            val sakId = call.parameters["sakId"]?.toInt()
            if(sakId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("SakId mangler")
            } else {
                call.respond(service.hentBehandlingerForSak(sakId, getAccessToken(call)))
            }
        }
    }

    /*
    Skal hente persondata og sakene for denne personen?
     */
    route("personer") {
        get("{fnr}") {

            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    val accessToken = getAccessToken(call)
                    val list = service.hentPerson(fnr, accessToken)
                    call.respond(list)
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        /*
        Hente alle saker med metadata om saken
         */
        // Opprette saker på en person
        post("{fnr}/saker") {
            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    val accessToken = getAccessToken(call)
                    service.opprettSak(fnr, "barnepensjon", accessToken) // sakType blir nok en enum etter hvert
                    call.respond("Ok");
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }

}