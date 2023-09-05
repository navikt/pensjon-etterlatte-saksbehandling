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
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_GOSYS_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.gosysOppgaveId
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgave.GosysOppgaveService

internal fun Route.oppgaveRoutesNy(
    service: OppgaveServiceNy,
    gosysOppgaveService: GosysOppgaveService
) {
    route("/api/nyeoppgaver") {
        get {
            kunSaksbehandler {
                call.respond(
                    service.finnOppgaverForBruker(
                        Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
                    )
                )
            }
        }

        get("{$BEHANDLINGSID_CALL_PARAMETER}/hentsaksbehandler") {
            kunSaksbehandler {
                val saksbehandler = service.hentSaksbehandlerForBehandling(behandlingsId)
                if (saksbehandler != null) {
                    call.respond(saksbehandler)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        get("{$BEHANDLINGSID_CALL_PARAMETER}/oppgaveunderarbeid") {
            kunSaksbehandler {
                val saksbehandler = service.hentSaksbehandlerForOppgaveUnderArbeid(behandlingsId)
                if (saksbehandler != null) {
                    call.respond(saksbehandler)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        route("{$OPPGAVEID_CALL_PARAMETER}") {
            post("tildel-saksbehandler") {
                val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                service.tildelSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
                call.respond(HttpStatusCode.OK)
            }
            post("bytt-saksbehandler") {
                kunSaksbehandler {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                    inTransaction {
                        service.byttSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
            route("saksbehandler", HttpMethod.Delete) {
                handle {
                    kunSaksbehandler {
                        service.fjernSaksbehandler(oppgaveId)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            put("frist") {
                kunSaksbehandler {
                    val redigerFrist = call.receive<RedigerFristRequest>()
                    service.redigerFrist(oppgaveId, redigerFrist.frist)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/gosys") {
            get {
                kunSaksbehandler {
                    call.respond(gosysOppgaveService.hentOppgaver(brukerTokenInfo))
                }
            }
            route("{$OPPGAVEID_GOSYS_CALL_PARAMETER}") {
                post("tildel-saksbehandler") {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringGosysDto>()
                    gosysOppgaveService.tildelOppgaveTilSaksbehandler(
                        gosysOppgaveId,
                        saksbehandlerEndringDto.versjon,
                        saksbehandlerEndringDto.saksbehandler,
                        brukerTokenInfo
                    )
                    call.respond(HttpStatusCode.OK)
                }
                post("endre-frist") {
                    val redigerFristRequest = call.receive<RedigerFristGosysRequest>()
                    gosysOppgaveService.endreFrist(
                        gosysOppgaveId,
                        redigerFristRequest.versjon,
                        redigerFristRequest.frist,
                        brukerTokenInfo
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}