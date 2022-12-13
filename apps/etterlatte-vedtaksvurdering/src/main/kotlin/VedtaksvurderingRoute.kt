package no.nav.etterlatte

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.navIdent
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("RouteApi")
fun Route.vedtaksvurderingRoute(service: VedtaksvurderingService) {
    route("api") {
        get("behandlinger/{behandlingId}/vedtak") {
            withBehandlingId { behandlingId ->
                val vedtaksresultat = service.hentVedtak(behandlingId)
                if (vedtaksresultat == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtaksresultat)
                }
            }
        }

        get("behandlinger/{behandlingId}/fellesvedtak") {
            withBehandlingId { behandlingId ->
                val vedtaksresultat = service.hentFellesvedtak(
                    behandlingId = behandlingId
                )
                if (vedtaksresultat == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtaksresultat)
                }
            }
        }

        post("vedtak/upsert/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val nyttVedtak = service.opprettEllerOppdaterVedtak(
                    behandlingId = behandlingId,
                    accessToken = accesstoken
                )
                call.respond(nyttVedtak)
            }
        }

        post("vedtak/attester/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val saksbehandler = call.navIdent
                val attestert = service.attesterVedtak(behandlingId, saksbehandler)
                call.respond(attestert)
            }
        }

        post("vedtak") {
            logger.debug("Request om vedtak bolk")
            val request = call.receive<VedtakBolkRequest>()
            val vedtak = service.hentVedtakBolk(request.behandlingsidenter.map { UUID.fromString(it) })
            call.respond(VedtakBolkResponse(vedtak))
        }

        post("vedtak/fattvedtak/{behandlingId}") {
            withBehandlingId { behandlingId ->
                call.respond(service.fattVedtak(behandlingId, call.navIdent))
            }
        }

        post("vedtak/underkjenn/{behandlingId}") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val begrunnelse = call.receive<UnderkjennVedtakClientRequest>()
            val underkjentVedtak = service.underkjennVedtak(behandlingId, begrunnelse)
            call.respond(underkjentVedtak)
        }
    }
}

data class UnderkjennVedtakClientRequest(val kommentar: String, val valgtBegrunnelse: String)
private data class VedtakBolkRequest(val behandlingsidenter: List<String>)
private data class VedtakBolkResponse(val vedtak: List<Vedtak>)