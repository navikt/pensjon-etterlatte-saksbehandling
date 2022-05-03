package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.Api(vedtaksvurderingService: VedtaksvurderingService) {
    get("hentVilkaarsresultat/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = call.parameters["behandlingId"].toString()
        val vilkaarsresultat = vedtaksvurderingService.hentVilkaarsresultat(sakId, behandlingId)
        call.respond(vilkaarsresultat)
    }
    get("hentBeregningsresultat") {

    }
    get("hentAvkortningsresultat") {

    }
    post("fattVedtak") {

    }
}