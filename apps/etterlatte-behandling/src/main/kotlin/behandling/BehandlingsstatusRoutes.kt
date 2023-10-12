package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.tilgangsstyring.kunAttestant
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse

internal fun Route.behandlingsstatusRoutes(behandlingsstatusService: BehandlingStatusService) {
    route("/behandlinger/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get("/opprett") {
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settOpprettet(behandlingsId)
                }
            }
        }
        post("/opprett") {
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settOpprettet(behandlingsId, false)
                }
            }
        }

        get("/vilkaarsvurder") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settVilkaarsvurdert(behandlingsId, true)
                }
            }
        }
        post("/vilkaarsvurder") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settVilkaarsvurdert(behandlingsId, false)
                }
            }
        }

        get("/oppdaterTrygdetid") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settTrygdetidOppdatert(behandlingsId, true)
                }
            }
        }
        post("/oppdaterTrygdetid") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settTrygdetidOppdatert(behandlingsId, false)
                }
            }
        }

        get("/beregn") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settBeregnet(behandlingsId)
                }
            }
        }

        post("/beregn") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settBeregnet(behandlingsId, false)
                }
            }
        }

        get("/avkort") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settAvkortet(behandlingsId)
                }
            }
        }

        post("/avkort") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settAvkortet(behandlingsId, false)
                }
            }
        }

        get("/fatteVedtak") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanFatteVedtak(behandlingsId)
                }
            }
        }
        get("/returner") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanReturnereVedtak(behandlingsId)
                }
            }
        }

        get("/attester") {
            kunAttestant {
                haandterStatusEndring(call) {
                    inTransaction {
                        behandlingsstatusService.sjekkOmKanAttestere(behandlingsId)
                    }
                }
            }
        }

        post("/iverksett") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settIverksattVedtak(behandlingsId, vedtakHendelse)
                }
            }
        }
    }

    route("/behandlinger") {
        post("/settTilbakeTilVilkaarsvurdert") {
            val tilbakestilteBehandlinger =
                behandlingsstatusService.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()
            call.respond(tilbakestilteBehandlinger)
        }
    }
}

data class OperasjonGyldig(val gyldig: Boolean)

private suspend fun haandterStatusEndring(
    call: ApplicationCall,
    proevStatusEndring: () -> Unit,
) {
    runCatching(proevStatusEndring)
        .fold(
            onSuccess = { call.respond(HttpStatusCode.OK, OperasjonGyldig(true)) },
            onFailure = { throw BehandlingKanIkkeBytteStatusException() },
        )
}

class BehandlingKanIkkeBytteStatusException : ForespoerselException(
    status = HttpStatusCode.Conflict.value,
    code = "BEHANDLING_HAR_UGYLDIG_STATUS",
    detail = "Behandlingen kan ikke bytte til ønsket status",
)
