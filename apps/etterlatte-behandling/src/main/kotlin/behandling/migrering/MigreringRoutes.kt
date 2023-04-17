package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import rapidsandrivers.migrering.MigreringRequest

fun Route.migreringRoutes(
    migreringService: MigreringService
) {
    route("/migrering") {
        post {
            val request = call.receive<MigreringRequest>()
            val behandling = migreringService.migrer(request)

            when (behandling) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(HttpStatusCode.Companion.Created, behandling.id)
            }
        }
    }
}