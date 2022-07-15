package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import no.nav.etterlatte.behandling.OppgaveService
import io.ktor.routing.route
import io.ktor.routing.get

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