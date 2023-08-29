package no.nav.etterlatte.oppgaveny

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.oppgaveNy.GosysOppgave
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgaveNy.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgaveNy.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgave.GosysOppgaveService
import java.net.SocketException

internal fun Route.oppgaveRoutesNy(
    service: OppgaveServiceNy,
    gosysOppgaveService: GosysOppgaveService,
    kanBrukeNyOppgaveliste: Boolean
) {
    val logger = application.log
    route("/api/nyeoppgaver") {
        get {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    coroutineScope {
                        val oppgaver = async {
                            service.finnOppgaverForBruker(
                                Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller
                            )
                        }
                        var gosysOppgaver = emptyList<GosysOppgave>()
                        try {
                            gosysOppgaver = async {
                                gosysOppgaveService.hentOppgaver(brukerTokenInfo)
                            }.await()
                        } catch (e: SocketException) {
                            logger.error("Feil mot gosys ved oppgavehenting", e)
                        }

                        call.respond(
                            oppgaver.await() + gosysOppgaver
                        )
                    }
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }

        get("{$BEHANDLINGSID_CALL_PARAMETER}/hentsaksbehandler") {
            kunSaksbehandler {
                if (kanBrukeNyOppgaveliste) {
                    val saksbehandler = service.hentSaksbehandlerForBehandling(behandlingsId)
                    if (saksbehandler != null) {
                        call.respond(saksbehandler)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
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
                        inTransaction {
                            service.byttSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
                        }
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

        route("/gosys/{gosysOppgaveId}") {
            post("tildel-saksbehandler") {
                if (kanBrukeNyOppgaveliste) {
                    val gosysOppgaveId = call.parameters["gosysOppgaveId"]!!
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringGosysDto>()
                    gosysOppgaveService.tildelOppgaveTilSaksbehandler(
                        gosysOppgaveId,
                        saksbehandlerEndringDto.versjon,
                        saksbehandlerEndringDto.saksbehandler,
                        brukerTokenInfo
                    )
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
            post("endre-frist") {
                if (kanBrukeNyOppgaveliste) {
                    val gosysOppgaveId = call.parameters["gosysOppgaveId"]!!
                    val redigerFristRequest = call.receive<RedigerFristGosysRequest>()
                    gosysOppgaveService.endreFrist(
                        gosysOppgaveId,
                        redigerFristRequest.versjon,
                        redigerFristRequest.frist,
                        brukerTokenInfo
                    )
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
    }
}