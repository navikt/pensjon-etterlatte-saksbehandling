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
import no.nav.etterlatte.libs.common.oppgaveNy.FjernSaksbehandlerRequest
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OpprettNyOppgaveRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto

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
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                    service.tildelSaksbehandler(saksbehandlerEndringDto)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
        post("bytt-saksbehandler") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                    service.byttSaksbehandler(saksbehandlerEndringDto)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
        post("fjern-saksbehandler") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    val fjernSaksbehandlerRequest = call.receive<FjernSaksbehandlerRequest>()
                    service.fjernSaksbehandler(fjernSaksbehandlerRequest)
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
        post("/opprett") {
            if (kanBrukeNyOppgaveliste) {
                val oppgaveRequest = call.receive<OpprettNyOppgaveRequest>()
                service.opprettNyOppgaveMedSakOgReferanse(
                    oppgaveRequest.referanse,
                    oppgaveRequest.sakId,
                    oppgaveRequest.oppgaveType
                )
                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}