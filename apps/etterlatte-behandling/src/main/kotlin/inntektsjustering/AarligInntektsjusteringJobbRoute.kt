package no.nav.etterlatte.inntektsjustering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest

internal fun Route.aarligInntektsjusteringRoute(service: AarligInntektsjusteringJobbService) {
    route("/inntektsjustering") {
        post("aarlig-jobb") {
            val request = call.receive<AarligInntektsjusteringRequest>()
            service.startAarligInntektsjusteringJobb(request)
            call.respond(HttpStatusCode.OK)
        }
    }
}
