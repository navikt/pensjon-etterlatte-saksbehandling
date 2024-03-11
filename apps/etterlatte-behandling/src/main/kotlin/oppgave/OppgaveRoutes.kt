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
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.kunSaksbehandler
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.oppgave.EndrePaaVentRequest
import no.nav.etterlatte.libs.common.oppgave.FerdigstillRequest
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.oppgave.VentefristerGaarUtResponse
import no.nav.etterlatte.libs.common.oppgaveId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
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

inline val PipelineContext<*, ApplicationCall>.minOppgavelisteidentQueryParam: String?
    get() {
        val minOppgavelisteIdentFilter = call.request.queryParameters["kunInnloggetOppgaver"].toBoolean()
        return if (minOppgavelisteIdentFilter) {
            brukerTokenInfo.ident()
        } else {
            null
        }
    }

internal fun Route.oppgaveRoutes(service: OppgaveService) {
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
                            minOppgavelisteidentQueryParam,
                        )
                    },
                )
            }
        }

        get("/referanse/{referanse}") {
            kunSaksbehandler {
                call.respond(
                    inTransaction {
                        service.hentOppgaverForReferanse(referanse)
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
                    val saksbehandler =
                        inTransaction {
                            service.hentSisteSaksbehandlerIkkeAttestertOppgave(referanse)
                        }
                    call.respond(saksbehandler ?: HttpStatusCode.NoContent)
                }
            }
            get("/ikkeattestertOppgave/{referanse}") {
                kunSaksbehandler {
                    val oppgave = inTransaction { service.hentSisteIkkeAttestertOppgave(referanse) }
                    call.respond(oppgave)
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
                    val merknad = call.receive<FerdigstillRequest>().merknad
                    inTransaction {
                        service.hentOgFerdigstillOppgaveById(oppgaveId, brukerTokenInfo, merknad)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("sett-paa-vent") {
                kunSkrivetilgang {
                    val settPaaVentRequest = call.receive<EndrePaaVentRequest>()
                    inTransaction {
                        service.endrePaaVent(oppgaveId, settPaaVentRequest.merknad, settPaaVentRequest.paaVent)
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
                kunSkrivetilgang {
                    val redigerFrist = call.receive<RedigerFristRequest>()
                    inTransaction { service.redigerFrist(oppgaveId, redigerFrist.frist) }
                    call.respond(HttpStatusCode.OK)
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
                            nyOppgaveDto.frist,
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

        put("ventefrist-gaar-ut") {
            val request = call.receive<VentefristGaarUtRequest>()
            val oppgaver =
                inTransaction {
                    val oppgaver = service.hentFristGaarUt(request)
                    oppgaver.forEach {
                        service.oppdaterStatusOgMerknad(
                            it.oppgaveID,
                            "",
                            Status.UNDER_BEHANDLING,
                        )
                    }
                    oppgaver
                }
            call.respond(VentefristerGaarUtResponse(oppgaver))
        }
    }
}

internal data class GosysOppgaveversjon(val versjon: Long)
