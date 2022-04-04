package no.nav.etterlatte.behandling

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.getAccessToken

fun Route.vedtakRoute(service: VedtakService) {

    route("vedtak") {
        post("{behandlingId}"){
            val behandlingId = call.parameters["behandlingId"].toString()
            service.fattVedtak(behandlingId, getAccessToken(call))
        }
    }

}