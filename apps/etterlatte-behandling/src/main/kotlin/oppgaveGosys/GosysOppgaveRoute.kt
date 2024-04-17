package no.nav.etterlatte.oppgaveGosys

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.oppgave.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_GOSYS_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.gosysOppgaveId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.oppgave.GosysOppgaveversjon

internal fun Route.gosysOppgaveRoute(gosysService: GosysOppgaveService) {
    route("/api/oppgaver/gosys") {
        get {
            kunSaksbehandler {
                val tema = call.request.queryParameters.getAll("tema") ?: listOf("EYO", "EYB")

                call.respond(gosysService.hentOppgaver(tema, brukerTokenInfo))
            }
        }

        get("/journalfoering/{journalpostId}") {
            call.respond(gosysService.hentJournalfoeringsoppgave(call.parameters["journalpostId"]!!, brukerTokenInfo))
        }

        route("{$OPPGAVEID_GOSYS_CALL_PARAMETER}") {
            get {
                kunSaksbehandler {
                    val oppgave = gosysService.hentOppgave(gosysOppgaveId.toLong(), brukerTokenInfo)

                    call.respond(oppgave ?: HttpStatusCode.NoContent)
                }
            }

            post("tildel-saksbehandler") {
                kunSaksbehandler {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringGosysDto>()
                    val oppdatertVersjon =
                        gosysService.tildelOppgaveTilSaksbehandler(
                            gosysOppgaveId,
                            saksbehandlerEndringDto.versjon,
                            saksbehandlerEndringDto.saksbehandler,
                            brukerTokenInfo,
                        )
                    call.respond(GosysOppgaveversjon(oppdatertVersjon))
                }
            }

            post("endre-frist") {
                kunSaksbehandler {
                    val redigerFristRequest = call.receive<RedigerFristGosysRequest>()
                    val oppdatertVersjon =
                        gosysService.endreFrist(
                            gosysOppgaveId,
                            redigerFristRequest.versjon,
                            redigerFristRequest.frist,
                            brukerTokenInfo,
                        )
                    call.respond(GosysOppgaveversjon(oppdatertVersjon))
                }
            }

            post("ferdigstill") {
                kunSaksbehandler {
                    val versjon = call.request.queryParameters["versjon"]!!.toLong()

                    call.respond(gosysService.ferdigstill(gosysOppgaveId, versjon, brukerTokenInfo))
                }
            }

            post("feilregistrer") {
                kunSaksbehandler {
                    val request = call.receive<FeilregistrerOppgaveRequest>()

                    call.respond(gosysService.feilregistrer(gosysOppgaveId, request, brukerTokenInfo))
                }
            }
        }
    }
}

data class FeilregistrerOppgaveRequest(
    val beskrivelse: String,
    val versjon: Long,
)
