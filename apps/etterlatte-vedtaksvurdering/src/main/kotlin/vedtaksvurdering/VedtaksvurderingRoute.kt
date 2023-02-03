package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.libs.ktor.saksbehandler
import java.util.*

fun Route.vedtaksvurderingRoute(service: VedtaksvurderingService) {
    route("api") {
        val logger = application.log

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

        get("vedtak/sammendrag/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val vedtaksresultat = service.hentVedtak(behandlingId)?.toVedtakSammendrag()
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
                val attestert = service.attesterVedtak(behandlingId, saksbehandler.ident, accesstoken)

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
                val fattetVedtak = service.fattVedtak(behandlingId, saksbehandler.ident, accesstoken)

                call.respond(fattetVedtak)
            }
        }

        post("vedtak/underkjenn/{behandlingId}") {
            val behandlingId = UUID.fromString(call.parameters["behandlingId"])
            val begrunnelse = call.receive<UnderkjennVedtakClientRequest>()
            val underkjentVedtak = service.underkjennVedtak(
                behandlingId,
                accesstoken,
                saksbehandler,
                begrunnelse
            )

            call.respond(underkjentVedtak)
        }
    }
}

data class UnderkjennVedtakClientRequest(val kommentar: String, val valgtBegrunnelse: String)
private data class VedtakBolkRequest(val behandlingsidenter: List<String>)
private data class VedtakBolkResponse(val vedtak: List<Vedtak>)

enum class HendelseType {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT
}