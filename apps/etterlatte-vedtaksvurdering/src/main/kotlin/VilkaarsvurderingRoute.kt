package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("RouteApi")
fun Route.vilkaarsvurderingRoute(service: VedtaksvurderingService) {
    get("hentvedtak/{sakId}/{behandlingId}") { // TODO: ubrukt sj?
        val behandlingId = UUID.fromString(call.parameters["behandlingId"])
        val vedtaksresultat = service.hentVedtak(behandlingId)
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

    get("behandlinger/{behandlingId}/fellesvedtak") {
        val behandlingId = UUID.fromString(call.parameters["behandlingId"])
        val vedtaksresultat = service.populerOgHentFellesVedtak(
            behandlingId = behandlingId,
            accessToken = accesstoken
        )
        if (vedtaksresultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(vedtaksresultat)
        }
    }

    post("vedtak") {
        logger.debug("Request om vedtak bolk")
        val request = call.receive<VedtakBolkRequest>()
        val vedtak = service.hentVedtakBolk(request.behandlingsidenter.map { UUID.fromString(it) })
        call.respond(VedtakBolkResponse(vedtak))
    }
}
private data class VedtakBolkRequest(val behandlingsidenter: List<String>)
private data class VedtakBolkResponse(val vedtak: List<Vedtak>)