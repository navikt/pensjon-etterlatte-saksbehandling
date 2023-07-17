package no.nav.etterlatte.oppgaveny

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.kunSaksbehandler

internal fun Route.oppgaveRoutesNy(service: OppgaveServiceNy, kanBrukeNyOppgaveliste: Boolean) {
    route("/api/nyeoppgaver") {
        get("/hent") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    call.respond(
                        service.finnOppgaverForBruker(Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller)
                    )
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }

        post("tildel-saksbehandler") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    val nySaksbehandlerDto = call.receive<NySaksbehandlerDto>()
                    service.tildelSaksbehandler(nySaksbehandlerDto)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
        post("bytt-saksbehandler") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    val nySaksbehandlerDto = call.receive<NySaksbehandlerDto>()
                    service.byttSaksbehandler(nySaksbehandlerDto)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
        post("/lagre") {
            if (kanBrukeNyOppgaveliste) {
                val oppgave = call.receive<OppgaveNy>()
                // service.lagreOppgave(oppgave)
                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}