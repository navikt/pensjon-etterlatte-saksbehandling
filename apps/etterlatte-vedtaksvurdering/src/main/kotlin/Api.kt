package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.*

fun Route.Api(service: VedtaksvurderingService) {
    get("hentvedtak/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = UUID.fromString(call.parameters["behandlingId"])
        val vedtaksresultat = service.hentVedtak(sakId, behandlingId)
        if (vedtaksresultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(vedtaksresultat)
        }
    }
    get("behandlinger/{behandlingId}/vedtak") {
        val behandlingId = UUID.fromString(call.parameters["behandlingId"])
        val vedtaksresultat = service.hentVedtak(behandlingId)
        if (vedtaksresultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(vedtaksresultat)
        }
    }
}