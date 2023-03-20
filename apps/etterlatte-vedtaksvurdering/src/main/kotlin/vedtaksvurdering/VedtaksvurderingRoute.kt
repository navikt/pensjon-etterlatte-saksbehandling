package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

fun Route.vedtaksvurderingRoute(service: VedtaksvurderingService, behandlingKlient: BehandlingKlient) {
    route("/api/vedtak") {
        val logger = application.log

        get("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter vedtak for behandling $behandlingId")
                val vedtak = service.hentVedtak(behandlingId)
                if (vedtak == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtak.toDto())
                }
            }
        }

        get("/{$BEHANDLINGSID_CALL_PARAMETER}/sammendrag") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter sammendrag av vedtak for behandling $behandlingId")
                val vedtaksresultat = service.hentVedtak(behandlingId)?.toVedtakSammendragDto()
                if (vedtaksresultat == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtaksresultat)
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/upsert") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Oppretter eller oppdaterer vedtak for behandling $behandlingId")
                val nyttVedtak = service.opprettEllerOppdaterVedtak(behandlingId, bruker)
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/fattvedtak") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Fatter vedtak for behandling $behandlingId")
                val fattetVedtak = service.fattVedtak(behandlingId, bruker)

                call.respond(fattetVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/attester") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Attesterer vedtak for behandling $behandlingId")
                val (kommentar) = call.receive<AttesterVedtakDto>()
                val attestert = service.attesterVedtak(behandlingId, kommentar, bruker)

                call.respond(attestert.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/underkjenn") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Underkjenner vedtak for behandling $behandlingId")
                val begrunnelse = call.receive<UnderkjennVedtakDto>()
                val underkjentVedtak = service.underkjennVedtak(
                    behandlingId,
                    bruker,
                    begrunnelse
                )

                call.respond(underkjentVedtak.toDto())
            }
        }

        get("/loepende/{$SAKID_CALL_PARAMETER}") {
            withSakId(behandlingKlient) { sakId ->
                val dato = call.request.queryParameters["dato"]?.let { LocalDate.parse(it) }
                    ?: throw Exception("dato er påkrevet på formatet YYYY-MM-DD")

                logger.info("Sjekker om vedtak er løpende for sak $sakId på dato $dato")
                val loependeYtelse = service.sjekkOmVedtakErLoependePaaDato(sakId, dato)
                call.respond(loependeYtelse.toDto())
            }
        }

        patch("/{$BEHANDLINGSID_CALL_PARAMETER}/tilbakestill") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Tilbakestiller ikke iverksatte vedtak for behandling $behandlingId")
                service.tilbakestillIkkeIverksatteVedtak(behandlingId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun Vedtak.toVedtakSammendragDto() = VedtakSammendragDto(
    id = id.toString(),
    behandlingId = behandlingId,
    datoAttestert = attestasjon?.tidspunkt?.toNorskTid()
)

private fun LoependeYtelse.toDto() = LoependeYtelseDTO(
    erLoepende = erLoepende,
    dato = dato
)

data class UnderkjennVedtakDto(val kommentar: String, val valgtBegrunnelse: String)

data class VedtakSammendragDto(val id: String, val behandlingId: UUID, val datoAttestert: ZonedDateTime?)