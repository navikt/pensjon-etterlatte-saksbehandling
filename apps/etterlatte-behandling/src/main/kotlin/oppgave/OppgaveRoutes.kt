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
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.FerdigstillRequest
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveBulkDto
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.RedigerFristRequest
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.oppgave.VentefristerGaarUtResponse
import no.nav.etterlatte.libs.ktor.route.OPPGAVEID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.oppgaveId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.util.UUID

inline val PipelineContext<*, ApplicationCall>.referanse: String
    get() =
        call.parameters["referanse"] ?: throw UgyldigForespoerselException(
            code = "REFERANSE_MANGLER",
            detail = "Mangler referanse i forespørselen",
        )

inline val PipelineContext<*, ApplicationCall>.gruppeId: String
    get() =
        call.parameters["gruppeId"] ?: throw UgyldigForespoerselException(
            code = "GRUPPEID_MANGLER",
            detail = "Mangler gruppeId i forespørselen",
        )

const val VISALLE = "VISALLE"

fun filtrerGyldigeStatuser(statuser: List<String>?): List<String> =
    statuser
        ?.map { i -> i.uppercase() }
        ?.filter { i -> Status.entries.map { it.name }.contains(i) || i == VISALLE } ?: emptyList()

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

        get("/gruppe/{gruppeId}") {
            kunSaksbehandler {
                val type = OppgaveType.valueOf(call.request.queryParameters["type"]!!)

                val oppgaver =
                    inTransaction {
                        service.hentOppgaverForGruppeId(gruppeId, type)
                    }

                call.respond(oppgaver)
            }
        }

        get("/referanse/{referanse}/underbehandling") {
            kunSaksbehandler {
                val oppgave =
                    inTransaction {
                        service.hentOppgaveUnderBehandling(referanse)
                    }

                call.respond(oppgave ?: HttpStatusCode.NoContent)
            }
        }

        route("/stats") {
            get {
                kunSaksbehandler {
                    call.respond(
                        inTransaction {
                            service.genererStatsForOppgaver(Kontekst.get().AppUser.name())
                        },
                    )
                }
            }
        }

        route("/bulk") {
            post("/opprett") {
                kunSaksbehandler {
                    val oppgaveBulkDto = call.receive<NyOppgaveBulkDto>()
                    inTransaction {
                        service.opprettOppgaveBulk(
                            "",
                            oppgaveBulkDto.sakIds,
                            oppgaveBulkDto.kilde,
                            oppgaveBulkDto.type,
                            oppgaveBulkDto.merknad,
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/tildel") {
                kunSaksbehandler {
                    val request = call.receive<TildelingBulkRequest>()

                    inTransaction {
                        request.oppgaver
                            .forEach { oppgaveId ->
                                service.tildelSaksbehandler(oppgaveId, request.saksbehandler)
                            }
                    }

                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get("/oppgaver") {
                kunSaksbehandler {
                    call.respond(inTransaction { service.hentOppgaverForSak(sakId) })
                }
            }

            post("/opprett") {
                kunSaksbehandlerMedSkrivetilgang {
                    val nyOppgaveDto = call.receive<NyOppgaveDto>()

                    val nyOppgave =
                        inTransaction {
                            service.opprettOppgave(
                                referanse = nyOppgaveDto.referanse ?: "",
                                sakId = sakId,
                                kilde = nyOppgaveDto.oppgaveKilde,
                                type = nyOppgaveDto.oppgaveType,
                                merknad = nyOppgaveDto.merknad,
                                frist = null,
                                saksbehandler = nyOppgaveDto.saksbehandler,
                            )
                        }

                    call.respond(nyOppgave)
                }
            }
        }

        route("{$OPPGAVEID_CALL_PARAMETER}") {
            get {
                val oppgave =
                    inTransaction {
                        service.hentOppgave(oppgaveId)
                    }

                call.respond(oppgave)
            }

            get("endringer") {
                val endringer =
                    inTransaction {
                        service.hentEndringerOppgave(oppgaveId)
                    }

                call.respond(endringer)
            }

            put("ferdigstill") {
                kunSkrivetilgang {
                    val merknad = call.receive<FerdigstillRequest>().merknad
                    val oppgave =
                        inTransaction {
                            service.ferdigstillOppgave(oppgaveId, brukerTokenInfo, merknad)
                        }
                    call.respond(oppgave)
                }
            }

            post("sett-paa-vent") {
                kunSkrivetilgang {
                    val req = call.receive<EndrePaaVentRequest>()
                    val oppgave =
                        inTransaction {
                            service.endrePaaVent(oppgaveId, req.paaVent, req.merknad, req.aarsak)
                        }
                    call.respond(oppgave)
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
        get("/sak/{$SAKID_CALL_PARAMETER}/oppgaver") {
            kunSystembruker {
                call.respond(inTransaction { service.hentOppgaverForSak(sakId) })
            }
        }

        post("/sak/{$SAKID_CALL_PARAMETER}/opprett") {
            kunSystembruker {
                val nyOppgaveDto = call.receive<NyOppgaveDto>()
                call.respond(
                    inTransaction {
                        service.opprettOppgave(
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
                            it.oppgaveId,
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

internal data class TildelingBulkRequest(
    val saksbehandler: String,
    val oppgaver: List<UUID>,
)
