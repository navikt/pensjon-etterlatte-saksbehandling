package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.OPPGAVEID_GOSYS_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.gosysOppgaveId
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.RedigerFristGosysRequest
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringGosysDto
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveService
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

class ManglerReferanseException(msg: String) :
    UgyldigForespoerselException(
        code = "MÅ_HA_REFERANSE",
        detail = msg,
    )

inline val PipelineContext<*, ApplicationCall>.referanse: String
    get() =
        call.parameters["referanse"] ?: throw ManglerReferanseException(
            "Mangler referanse i requestem",
        )

const val VISALLE = "VISALLE"

fun filtrerGyldigeStatuser(statuser: List<String>?): List<String> {
    return statuser?.map { i -> i.uppercase() }
        ?.filter { i -> Status.entries.map { it.name }.contains(i) || i == VISALLE } ?: emptyList()
}

internal fun Route.oppgaveRoutes(
    service: OppgaveService,
    gosysOppgaveService: GosysOppgaveService,
) {
    route("/api/oppgaver") {
        get {
            kunSaksbehandler {
                val oppgaveStatuser = call.request.queryParameters.getAll("oppgaveStatus")
                val filtrerteStatuser = filtrerGyldigeStatuser(oppgaveStatuser)

                call.respond(
                    inTransaction {
                        service.finnOppgaverForBruker(
                            Kontekst.get().appUserAsSaksbehandler(),
                            filtrerteStatuser,
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

            post("/opprett") {
                kunSaksbehandlerMedSkrivetilgang {
                    val nyOppgaveDto = call.receive<NyOppgaveDto>()

                    val nyOppgave =
                        inTransaction {
                            service.opprettNyOppgaveMedSakOgReferanse(
                                nyOppgaveDto.referanse ?: "",
                                sakId,
                                nyOppgaveDto.oppgaveKilde,
                                nyOppgaveDto.oppgaveType,
                                nyOppgaveDto.merknad,
                            )
                        }

                    call.respond(nyOppgave)
                }
            }

            get("/oppgaveunderbehandling/{referanse}") {
                kunSaksbehandler {
                    val saksbehandler =
                        inTransaction {
                            service.hentSaksbehandlerForOppgaveUnderArbeidByReferanse(referanse)
                        }
                    call.respond(saksbehandler ?: HttpStatusCode.NoContent)
                }
            }

            get("/ikkeattestert/{referanse}") {
                kunSaksbehandler {
                    val saksbehandler = inTransaction { service.hentSisteSaksbehandlerIkkeAttestertOppgave(referanse) }
                    call.respond(saksbehandler)
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
                kunSkrivetilgang {
                    inTransaction {
                        service.hentOgFerdigstillOppgaveById(oppgaveId, brukerTokenInfo)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("tildel-saksbehandler") {
                kunSkrivetilgang {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                    inTransaction {
                        service.tildelSaksbehandler(
                            oppgaveId,
                            saksbehandlerEndringDto.saksbehandler,
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
            post("bytt-saksbehandler") {
                kunSaksbehandlerMedSkrivetilgang {
                    val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringDto>()
                    inTransaction {
                        service.byttSaksbehandler(oppgaveId, saksbehandlerEndringDto.saksbehandler)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
            delete("saksbehandler") {
                kunSaksbehandlerMedSkrivetilgang {
                    inTransaction { service.fjernSaksbehandler(oppgaveId) }
                    call.respond(HttpStatusCode.OK)
                }
            }
            put("frist") {
                kunSaksbehandlerMedSkrivetilgang {
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
                    kunSaksbehandlerMedSkrivetilgang {
                        val saksbehandlerEndringDto = call.receive<SaksbehandlerEndringGosysDto>()
                        val oppdatertVersjon =
                            gosysOppgaveService.tildelOppgaveTilSaksbehandler(
                                gosysOppgaveId,
                                saksbehandlerEndringDto.versjon,
                                saksbehandlerEndringDto.saksbehandler,
                                brukerTokenInfo,
                            )
                        call.respond(GosysOppgaveversjon(oppdatertVersjon))
                    }
                }

                post("endre-frist") {
                    kunSaksbehandlerMedSkrivetilgang {
                        val redigerFristRequest = call.receive<RedigerFristGosysRequest>()
                        val oppdatertVersjon =
                            gosysOppgaveService.endreFrist(
                                gosysOppgaveId,
                                redigerFristRequest.versjon,
                                redigerFristRequest.frist,
                                brukerTokenInfo,
                            )
                        call.respond(GosysOppgaveversjon(oppdatertVersjon))
                    }
                }
            }
        }
    }

    route("/oppgaver") {
        post("/sak/{$SAKID_CALL_PARAMETER}/opprett") {
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

        put("/avbryt/referanse/{referanse}") {
            kunSystembruker {
                val referanse = call.parameters["referanse"]!!

                inTransaction {
                    service.avbrytAapneOppgaverMedReferanse(referanse)
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

internal data class GosysOppgaveversjon(val versjon: Long)
