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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.tilgangsstyring.kunAttestant
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse

internal fun Route.behandlingsstatusRoutes(behandlingsstatusService: BehandlingStatusService) {
    route("/behandlinger/{$BEHANDLINGID_CALL_PARAMETER}") {
        get("/opprett") {
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settOpprettet(behandlingId)
                }
            }
        }
        post("/opprett") {
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settOpprettet(behandlingId, false)
                }
            }
        }

        get("/vilkaarsvurder") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settVilkaarsvurdert(behandlingId, true)
                }
            }
        }
        post("/vilkaarsvurder") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settVilkaarsvurdert(behandlingId, false)
                }
            }
        }

        get("/oppdaterTrygdetid") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settTrygdetidOppdatert(behandlingId, true)
                }
            }
        }
        post("/oppdaterTrygdetid") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settTrygdetidOppdatert(behandlingId, false)
                }
            }
        }

        get("/beregn") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settBeregnet(behandlingId)
                }
            }
        }

        post("/beregn") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settBeregnet(behandlingId, false)
                }
            }
        }

        get("/avkort") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settAvkortet(behandlingId)
                }
            }
        }

        post("/avkort") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settAvkortet(behandlingId, false)
                }
            }
        }

        get("/fatteVedtak") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanFatteVedtak(behandlingId)
                }
            }
        }
        get("/returner") {
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanReturnereVedtak(behandlingId)
                }
            }
        }

        get("/attester") {
            kunAttestant {
                haandterStatusEndring(call) {
                    inTransaction {
                        behandlingsstatusService.sjekkOmKanAttestere(behandlingId)
                    }
                }
            }
        }

        post("/iverksett") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                inTransaction {
                    behandlingsstatusService.settIverksattVedtak(behandlingId, vedtakHendelse)
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
