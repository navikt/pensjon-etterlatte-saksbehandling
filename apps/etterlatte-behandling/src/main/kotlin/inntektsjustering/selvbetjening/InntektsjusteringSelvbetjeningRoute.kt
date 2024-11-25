package no.nav.etterlatte.inntektsjustering.selvbetjening

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.inntektsjustering.InntektsjusteringRequest

internal fun Route.inntektsjusteringSelvbetjeningRoute(service: InntektsjusteringSelvbetjeningService) {
    route("/inntektsjustering") {
        post("behandle") {
            val request = call.receive<InntektsjusteringRequest>()
            service.behandleInntektsjustering(request)
            call.respond(HttpStatusCode.OK)
        }
    }
}
