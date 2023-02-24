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
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall

internal fun Route.behandlingsstatusRoutes(
    behandlingsstatusService: BehandlingStatusService
) {
    route("/behandlinger/{behandlingsid}") {
        get("/opprett") {
            /* Kalles kun av vilkårsvurdering når total-vurdering slettes */
            haandterStatusEndring(call) {
                behandlingsstatusService.settOpprettet(behandlingsId)
            }
        }
        post("/opprett") {
            /* Kalles kun av vilkårsvurdering når total-vurdering slettes */
            haandterStatusEndring(call) {
                behandlingsstatusService.settOpprettet(behandlingsId, false)
            }
        }

        get("/vilkaarsvurder") {
            haandterStatusEndring(call) {
                behandlingsstatusService.settVilkaarsvurdert(behandlingsId, true, null)
            }
        }
        post("/vilkaarsvurder") {
            val body = call.receive<TilVilkaarsvurderingJson>()

            haandterStatusEndring(call) {
                behandlingsstatusService.settVilkaarsvurdert(behandlingsId, false, body.utfall)
            }
        }

        get("/beregn") {
            haandterStatusEndring(call) {
                behandlingsstatusService.settBeregnet(behandlingsId)
            }
        }

        post("/beregn") {
            haandterStatusEndring(call) {
                behandlingsstatusService.settBeregnet(behandlingsId, false)
            }
        }

        get("/fatteVedtak") {
            haandterStatusEndring(call) {
                behandlingsstatusService.sjekkOmKanFatteVedtak(behandlingsId)
            }
        }
        post("/fatteVedtak") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                behandlingsstatusService.settFattetVedtak(behandlingsId, vedtakHendelse)
            }
        }
        get("/returner") {
            haandterStatusEndring(call) {
                behandlingsstatusService.sjekkOmKanReturnereVedtak(behandlingsId)
            }
        }
        post("/returner") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                behandlingsstatusService.settReturnertVedtak(behandlingsId, vedtakHendelse)
            }
        }

        get("/attester") {
            haandterStatusEndring(call) {
                behandlingsstatusService.sjekkOmKanAttestere(behandlingsId)
            }
        }
        post("/attester") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                behandlingsstatusService.settAttestertVedtak(behandlingsId, vedtakHendelse)
            }
        }

        post("/iverksett") {
            val vedtakHendelse = call.receive<VedtakHendelse>()
            haandterStatusEndring(call) {
                behandlingsstatusService.settIverksattVedtak(behandlingsId, vedtakHendelse)
            }
        }
    }

    route("/behandlinger") {
        post("/settTilbakeTilVilkaarsvurdert") {
            behandlingsstatusService.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class OperasjonGyldig(val gyldig: Boolean)

private suspend fun haandterStatusEndring(call: ApplicationCall, proevStatusEndring: () -> Unit) {
    runCatching(proevStatusEndring)
        .fold(
            onSuccess = { call.respond(HttpStatusCode.OK, OperasjonGyldig(true)) },
            onFailure = { call.respond(HttpStatusCode.Conflict, it.message ?: "Statussjekk feilet") }
        )
}

internal data class TilVilkaarsvurderingJson(val utfall: VilkaarsvurderingUtfall)