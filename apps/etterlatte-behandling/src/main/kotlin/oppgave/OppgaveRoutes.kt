package no.nav.etterlatte.oppgave

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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_GOSYS_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.gosysOppgaveId
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveService

internal fun Route.oppgaveRoutes(
    service: OppgaveService,
    gosysOppgaveService: GosysOppgaveService,
) {
    route("/api/oppgaver") {
        get {
            kunSaksbehandler {
                call.respond(
                    inTransaction {
                        service.finnOppgaverForBruker(
                            Kontekst.get().appUserAsSaksbehandler().saksbehandlerMedRoller,
                        )
                    },
                )
            }
        }

        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get("/oppgaver") {
                kunSystembruker {
                    call.respond(inTransaction { service.hentSakOgOppgaverForSak(sakId) })
                }
            }
        }

        route("behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
            get("/hentsaksbehandler") {
                kunSaksbehandler {
                    val saksbehandler = inTransaction { service.hentSaksbehandlerForBehandling(behandlingId) }
                    call.respond(saksbehandler ?: HttpStatusCode.NoContent)
                }
            }

            get("/oppgaveunderarbeid") {
                kunSaksbehandler {
                    val saksbehandler =
                        inTransaction {
                            service.hentSaksbehandlerForOppgaveUnderArbeidByReferanse(behandlingId.toString())
                        }
                    call.respond(saksbehandler ?: HttpStatusCode.NoContent)
                }
            }
        }

        route("{$OPPGAVEID_CALL_PARAMETER}") {
            get {
                val oppgave =
                    inTransaction {
                        service.hentOppgave(oppgaveId)
                    }

                call.respond(oppgave ?: HttpStatusCode.NoContent)
            }

            put("ferdigstill") {
                inTransaction {
                    service.hentOgFerdigstillOppgaveById(oppgaveId, brukerTokenInfo)
                }
                call.respond(HttpStatusCode.OK)
            }

            post("tildel-saksbehandler") {
                val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                inTransaction {
                    service.tildelSaksbehandler(
                        oppgaveId,
                        saksbehandlerEndringDto.saksbehandler,
                    )
                }
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
                        inTransaction { service.fjernSaksbehandler(oppgaveId) }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            put("frist") {
                kunSaksbehandler {
                    val redigerFrist = call.receive<RedigerFristRequest>()
                    inTransaction { service.redigerFrist(oppgaveId, redigerFrist.frist) }
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
                get {
                    kunSaksbehandler {
                        val oppgave = gosysOppgaveService.hentOppgave(gosysOppgaveId.toLong(), brukerTokenInfo)

                        call.respond(oppgave ?: HttpStatusCode.NoContent)
                    }
                }

                post("tildel-saksbehandler") {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringGosysDto>()
                    gosysOppgaveService.tildelOppgaveTilSaksbehandler(
                        gosysOppgaveId,
                        saksbehandlerEndringDto.versjon,
                        saksbehandlerEndringDto.saksbehandler,
                        brukerTokenInfo,
                    )
                    call.respond(HttpStatusCode.OK)
                }

                post("endre-frist") {
                    val redigerFristRequest = call.receive<RedigerFristGosysRequest>()
                    gosysOppgaveService.endreFrist(
                        gosysOppgaveId,
                        redigerFristRequest.versjon,
                        redigerFristRequest.frist,
                        brukerTokenInfo,
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    route("/oppgaver/sak/{$SAKID_CALL_PARAMETER}/opprett") {
        post {
            kunSystembruker {
                val nyOppgaveDto = call.receive<NyOppgaveDto>()
                call.respond(
                    inTransaction {
                        service.opprettNyOppgaveMedSakOgReferanse(
                            nyOppgaveDto.referanse ?: "",
                            sakId,
                            nyOppgaveDto.oppgaveKilde,
                            nyOppgaveDto.oppgaveType,
                            nyOppgaveDto.merknad,
                        )
                    },
                )
            }
        }
    }
}
