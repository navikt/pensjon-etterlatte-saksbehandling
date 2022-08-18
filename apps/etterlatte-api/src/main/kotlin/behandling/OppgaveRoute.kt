package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.OppgaveService

fun Route.oppgaveRoute(service: OppgaveService) {
    route("oppgaver") {
        // hent liste med oppgaver til oppgavelista

        get {
            try {
                val accessToken = getAccessToken(call)
                val list = service.hentAlleOppgaver(accessToken)
                call.respond(list)
            } catch (e: Exception) {
                throw e
            }
        }
    }
}