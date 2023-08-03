package no.nav.etterlatte.oppgaveny

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto

internal fun Route.oppgaveRoutesNy(
    service: OppgaveServiceNy,
    kanBrukeNyOppgaveliste: Boolean
) {
    route("/api/nyeoppgaver") {
        get {
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

        route("{$OPPGAVEID_CALL_PARAMETER}") {
            post("tildel-saksbehandler") {
                kunSaksbehandler {
                    if (kanBrukeNyOppgaveliste) {
                        val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                        service.tildelSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
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
                        service.byttSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotImplemented)
                    }
                }
            }
            route("saksbehandler", HttpMethod.Delete) {
                handle {
                    kunSaksbehandler {
                        if (kanBrukeNyOppgaveliste) {
                            service.fjernSaksbehandler(oppgaveId)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotImplemented)
                        }
                    }
                }
            }
            put("frist") {
                kunSaksbehandler {
                    if (kanBrukeNyOppgaveliste) {
                        val redigerFrist = call.receive<RedigerFristRequest>()
                        service.redigerFrist(oppgaveId, redigerFrist.frist)
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotImplemented)
                    }
                }
            }
        }
    }
}