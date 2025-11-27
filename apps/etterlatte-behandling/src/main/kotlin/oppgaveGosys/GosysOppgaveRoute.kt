package no.nav.etterlatte.oppgaveGosys

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.oppgave.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_GOSYS_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.gosysOppgaveId
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

internal fun Route.gosysOppgaveRoute(gosysService: GosysOppgaveService) {
    route("/api/oppgaver/gosys") {
        get {
            kunSaksbehandler {
                val saksbehandler = call.request.queryParameters["saksbehandler"].takeUnless { it.isNullOrBlank() }
                val tema = call.request.queryParameters["tema"].takeUnless { it.isNullOrBlank() }
                val enhet = Enhetsnummer.nullable(call.request.queryParameters["enhet"])
                val harTildeling =
                    call.request.queryParameters["harTildeling"]
                        .takeUnless { it.isNullOrBlank() }
                        ?.toBooleanStrictOrNull()

                call.respond(gosysService.hentOppgaver(saksbehandler, tema, enhet, harTildeling, brukerTokenInfo))
            }
        }

        get("/journalfoering/{journalpostId}") {
            call.respond(gosysService.hentJournalfoeringsoppgave(call.parameters["journalpostId"]!!, brukerTokenInfo))
        }

        post("/person") {
            kunSaksbehandler {
                val request = call.receive<FoedselsnummerDTO>()

                call.respond(gosysService.hentOppgaverForPerson(request.foedselsnummer, brukerTokenInfo))
            }
        }

        route("{$OPPGAVEID_GOSYS_CALL_PARAMETER}") {
            get {
                kunSaksbehandler {
                    val oppgave = gosysService.hentOppgave(gosysOppgaveId.toLong(), brukerTokenInfo)

                    call.respond(oppgave ?: HttpStatusCode.NoContent)
                }
            }

            post("/flytt-til-gjenny") {
                kunSaksbehandler {
                    val flyttTilGjennyRequest = call.receive<FlyttOppgavetilGjennyRequest>()
                    val sakId =
                        flyttTilGjennyRequest.sakid
                    val nyOppgave = gosysService.flyttTilGjenny(gosysOppgaveId.toLong(), flyttTilGjennyRequest, brukerTokenInfo)
                    call.respond(nyOppgave)
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
                    val ferdigstillGosysOppgaveRequest = call.receive<FerdigstillGosysOppgaveRequest>()
                    //val versjon = call.request.queryParameters["versjon"]!!.toLong()
                    call.respond(gosysService.ferdigstill(
                        oppgaveId = gosysOppgaveId,
                        brukerTokenInfo = brukerTokenInfo,
                        request = ferdigstillGosysOppgaveRequest
                    ))
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
    val enhetsnr: String
)

internal data class GosysOppgaveversjon(
    val versjon: Long,
)

data class FerdigstillGosysOppgaveRequest(
    val versjon: Long,
    val enhetsnr: String,
)

data class FlyttOppgavetilGjennyRequest(
    val sakid: Long,
    val enhetsnr: String,
)