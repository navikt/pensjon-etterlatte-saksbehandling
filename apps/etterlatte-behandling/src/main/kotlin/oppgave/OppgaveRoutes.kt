package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsOppgave

data class OppgaveListeDto(
    val oppgaver: List<Oppgave>
)
data class GrunnlagsendringsoppgaverDto(val oppgaver: List<GrunnlagsendringsOppgave>)

fun Route.oppgaveRoutes(service: OppgaveService) {
    route("/oppgaver") {
        get {
            when (val bruker = Kontekst.get().AppUser) {
                is Saksbehandler -> call.respond(OppgaveListeDto(service.finnOppgaverForBruker(bruker)))
                else -> call.respond(HttpStatusCode.Forbidden)
            }
        }

        route("/endringshendelser") {
            get {
                when (val bruker = Kontekst.get().AppUser) {
                    is Saksbehandler -> call.respond(
                        GrunnlagsendringsoppgaverDto(
                            service.finnOppgaverUhaandterteGrunnlagsendringshendelser(bruker)
                        )
                    )
                    else -> call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }
}