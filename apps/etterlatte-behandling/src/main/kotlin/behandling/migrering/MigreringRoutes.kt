package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.migreringRoutes(migreringService: MigreringService) {
    route("/migrering") {
        post {
            when (val behandling = migreringService.migrer(call.receive())) {
                null -> call.respond(HttpStatusCode.NotFound)
                else ->
                    call.respond(
                        HttpStatusCode.Companion.Created,
                        BehandlingOgSak(behandling.id, behandling.sak.id),
                    )
            }
        }
        put("/{$BEHANDLINGSID_CALL_PARAMETER}/avbryt") {
            migreringService.avbrytBehandling(behandlingsId, brukerTokenInfo)
            call.respond(HttpStatusCode.OK)
        }
    }
}
