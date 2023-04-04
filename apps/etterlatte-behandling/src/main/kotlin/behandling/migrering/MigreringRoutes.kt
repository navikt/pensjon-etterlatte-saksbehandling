package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import rapidsandrivers.migrering.MigreringRequest
import java.util.*

fun Route.migreringRoutes(
    migreringService: MigreringService
) {
    route("/migrering") {
        post {
            val request = call.receive<MigreringRequest>()
            val behandlingId = migreringService.migrer(request)
            call.respond(HttpStatusCode.Companion.Created, MigreringResponse(behandlingId.id, behandlingId.sak.id))
        }
    }
}

data class MigreringResponse(val behandlingId: UUID, val sakId: Long)