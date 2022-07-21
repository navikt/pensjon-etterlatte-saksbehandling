package no.nav.etterlatte

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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