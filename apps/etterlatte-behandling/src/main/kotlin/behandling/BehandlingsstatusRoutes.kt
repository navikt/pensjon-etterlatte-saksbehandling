package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall

internal fun Route.behandlingsstatusRoutes(behandlingsstatusService: BehandlingStatusService) {
    route("/behandlinger/{behandlingsid}") {
        get("/opprett") {
            behandlingsstatusService.settOpprettet(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/opprett") {
            behandlingsstatusService.settOpprettet(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/vilkaarsvurder") {
            behandlingsstatusService.settVilkaarsvurdert(behandlingsId, true, null)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/vilkaarsvurder") {
            val body = call.receive<TilVilkaarsvurderingJson>()

            behandlingsstatusService.settVilkaarsvurdert(behandlingsId, false, body.utfall)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/beregn") {
            behandlingsstatusService.settBeregnet(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }

        post("/beregn") {
            behandlingsstatusService.settBeregnet(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/fatteVedtak") {
            behandlingsstatusService.settFattetVedtak(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/fatteVedtak") {
            behandlingsstatusService.settFattetVedtak(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }
            .get("/returner") {
                behandlingsstatusService.settReturnert(behandlingsId)
                call.respond(HttpStatusCode.OK, "true")
            }
        post("/returner") {
            behandlingsstatusService.settReturnert(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/attester") {
            behandlingsstatusService.settAttestert(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/attester") {
            behandlingsstatusService.settAttestert(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/iverksett") {
            behandlingsstatusService.settIverksatt(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/iverksett") {
            behandlingsstatusService.settIverksatt(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }
    }
}

internal data class TilVilkaarsvurderingJson(val utfall: VilkaarsvurderingUtfall)