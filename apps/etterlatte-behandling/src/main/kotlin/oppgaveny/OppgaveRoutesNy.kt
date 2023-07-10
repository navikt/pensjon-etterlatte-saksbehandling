package no.nav.etterlatte.oppgaveny

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler

internal fun Route.oppgaveRoutesNy(service: OppgaveServiceNy) {
    route("/api/nyeoppgaver") {
        get {
            when (brukerTokenInfo) {
                is Saksbehandler -> call.respond(
                    service.finnOppgaverForBruker(Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller)
                )

                else -> call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}