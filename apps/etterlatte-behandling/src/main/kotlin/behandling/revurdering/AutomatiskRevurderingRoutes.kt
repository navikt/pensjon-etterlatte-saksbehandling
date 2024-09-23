package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.route.routeLogger

fun Route.automatiskRevurderingRoutes(service: AutomatiskRevurderingService) {
    val logger = routeLogger
    route("/automatisk-revurdering") {
        post {
            val request = call.receive<AutomatiskRevurderingRequest>()
            val response = service.oppprettRevurderingOgOppfoelging(request)
            call.respond(response)
        }
    }
}
