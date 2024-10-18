package no.nav.etterlatte.inntektsjustering

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth

internal fun Route.aarligInntektsjusteringRoute(service: AarligInntektsjusteringJobbService) {
    route("/inntektsjustering") {
        get("start") {
            val request = call.receive<AarligInntektsjusteringRequest>()
            service.startAarligInntektsjustering(request)
        }
    }
}

data class AarligInntektsjusteringRequest(
    val kjoering: String,
    val loependeFom: YearMonth,
    val saker: List<SakId>,
)
