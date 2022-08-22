package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler

data class OppgaveListeDto(
    val oppgaver: List<Oppgave>
)

fun Route.oppgaveRoutes(repo: OppgaveDao) {
    route("/oppgaver") {
        get {
            val bruker = Kontekst.get().AppUser

            if (bruker is Saksbehandler) {
                repo.finnOppgaverForRoller(
                    listOfNotNull(
                        Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
                        Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() }
                    )
                ).let { OppgaveListeDto(it) }
                    .also { call.respond(it) }
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}