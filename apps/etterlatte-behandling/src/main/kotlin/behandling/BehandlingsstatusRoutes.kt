package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunAttestant
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse

internal fun Route.behandlingsstatusRoutes(behandlingsstatusService: BehandlingStatusService) {
    route("/behandlinger/{$BEHANDLINGID_CALL_PARAMETER}") {
        get("/opprett") {
            // TODO: kan slettes
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.settOpprettet(behandlingId, brukerTokenInfo)
                }
            }
        }
        post("/opprett") {
            // TODO: kan slettes
            // Kalles kun av vilkårsvurdering når total-vurdering slettes
            kunSkrivetilgang {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settOpprettet(behandlingId, brukerTokenInfo, false)
                    }
                }
            }
        }

        get("/vilkaarsvurder") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.settVilkaarsvurdert(behandlingId, brukerTokenInfo)
                }
            }
        }
        post("/vilkaarsvurder") {
            kunSkrivetilgang {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settVilkaarsvurdert(behandlingId, brukerTokenInfo, false)
                    }
                }
            }
        }

        get("/oppdaterTrygdetid") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo)
                }
            }
        }
        post("/oppdaterTrygdetid") {
            kunSkrivetilgang {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, false)
                    }
                }
            }
        }

        get("/beregn") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.settBeregnet(behandlingId, brukerTokenInfo)
                }
            }
        }

        post("/beregn") {
            kunSkrivetilgang {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settBeregnet(behandlingId, brukerTokenInfo, false)
                    }
                }
            }
        }

        get("/avkort") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.settAvkortet(behandlingId, brukerTokenInfo)
                }
            }
        }

        post("/avkort") {
            kunSkrivetilgang {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settAvkortet(behandlingId, brukerTokenInfo, false)
                    }
                }
            }
        }

        get("/fatteVedtak") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanFatteVedtak(behandlingId)
                }
            }
        }
        get("/returner") {
            haandterStatusEndring {
                inTransaction {
                    behandlingsstatusService.sjekkOmKanReturnereVedtak(behandlingId)
                }
            }
        }

        get("/attester") {
            kunAttestant {
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.sjekkOmKanAttestere(behandlingId)
                    }
                }
            }
        }

        post("/tilsamordning") {
            kunSkrivetilgang {
                val vedtakHendelse = call.receive<VedtakHendelse>()
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settTilSamordnetVedtak(behandlingId, vedtakHendelse)
                    }
                }
            }
        }

        post("/samordnet") {
            kunSkrivetilgang {
                val vedtakHendelse = call.receive<VedtakHendelse>()
                haandterStatusEndring {
                    inTransaction {
                        behandlingsstatusService.settSamordnetVedtak(behandlingId, vedtakHendelse)
                    }
                }
            }
        }

        post("/iverksett") {
            kunSkrivetilgang {
                val vedtakHendelse = call.receive<VedtakHendelse>()
                haandterStatusEndring {
                    inTransaction {
                        runBlocking {
                            behandlingsstatusService.settIverksattVedtak(behandlingId, vedtakHendelse)
                        }
                    }
                }
            }
        }
    }

    route("/behandlinger") {
        post("/settTilbakeTilTrygdetidOppdatert") {
            kunSystembruker {
                val sakslisteDTO = call.receive<SakslisteDTO>()
                val tilbakestilteBehandlinger =
                    behandlingsstatusService.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakslisteDTO)
                call.respond(tilbakestilteBehandlinger)
            }
        }
    }
}

data class OperasjonGyldig(
    val gyldig: Boolean,
)

private suspend fun RoutingContext.haandterStatusEndring(proevStatusEndring: () -> Unit) {
    runCatching(proevStatusEndring)
        .fold(
            onSuccess = { call.respond(HttpStatusCode.OK, OperasjonGyldig(true)) },
            onFailure = { throw BehandlingKanIkkeBytteStatusException() },
        )
}

class BehandlingKanIkkeBytteStatusException :
    ForespoerselException(
        status = HttpStatusCode.Conflict.value,
        code = "BEHANDLING_HAR_UGYLDIG_STATUS",
        detail = "Behandlingen kan ikke bytte til ønsket status",
    )
