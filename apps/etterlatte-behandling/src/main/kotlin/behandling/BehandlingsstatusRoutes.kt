package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall

internal fun Route.behandlingsstatusRoutes(foerstegangsbehandlingService: FoerstegangsbehandlingService) {
    route("/behandlinger/{behandlingsid}") {
        get("/opprett") {
            foerstegangsbehandlingService.settOpprettet(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/opprett") {
            foerstegangsbehandlingService.settOpprettet(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/vilkaarsvurder") {
            foerstegangsbehandlingService.settVilkaarsvurdert(behandlingsId, true, null)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/vilkaarsvurder") {
            val body = call.receive<TilVilkaarsvurderingJson>()

            foerstegangsbehandlingService.settVilkaarsvurdert(behandlingsId, false, body.utfall)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/fatteVedtak") {
            foerstegangsbehandlingService.settFattetVedtak(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/fatteVedtak") {
            foerstegangsbehandlingService.settFattetVedtak(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/returner") {
            foerstegangsbehandlingService.settReturnert(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/returner") {
            foerstegangsbehandlingService.settReturnert(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/iverksett") {
            foerstegangsbehandlingService.settIverksatt(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }
        post("/iverksett") {
            foerstegangsbehandlingService.settIverksatt(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }

        get("/beregn") {
            foerstegangsbehandlingService.settBeregnet(behandlingsId)
            call.respond(HttpStatusCode.OK, "true")
        }

        post("/beregn") {
            foerstegangsbehandlingService.settBeregnet(behandlingsId, false)
            call.respond(HttpStatusCode.OK, "true")
        }
    }
}

internal data class TilVilkaarsvurderingJson(val utfall: VilkaarsvurderingUtfall)