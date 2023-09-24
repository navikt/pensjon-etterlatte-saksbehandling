package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
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
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

fun Route.vedtaksvurderingRoute(
    service: VedtaksvurderingService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = application.log

        get("/sak/{${SAKID_CALL_PARAMETER}}/iverksatte") {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter iverksatte vedtak for sak $sakId")
                val iverksatteVedtak = service.hentIverksatteVedtakISak(sakId)
                call.respond(iverksatteVedtak.map { it.toVedtakSammendragDto() })
            }
        }

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
                    call.response.status(HttpStatusCode.NoContent)
                } else {
                    call.respond(vedtaksresultat)
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/upsert") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Oppretter eller oppdaterer vedtak for behandling $behandlingId")
                val nyttVedtak = service.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/fattvedtak") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Fatter vedtak for behandling $behandlingId")
                val fattetVedtak = service.fattVedtak(behandlingId, brukerTokenInfo)

                call.respond(fattetVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/attester") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Attesterer vedtak for behandling $behandlingId")
                val (kommentar) = call.receive<AttesterVedtakDto>()
                val attestert = service.attesterVedtak(behandlingId, kommentar, brukerTokenInfo)

                call.respond(attestert.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/underkjenn") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Underkjenner vedtak for behandling $behandlingId")
                val begrunnelse = call.receive<UnderkjennVedtakDto>()
                val underkjentVedtak =
                    service.underkjennVedtak(
                        behandlingId,
                        brukerTokenInfo,
                        begrunnelse,
                    )

                call.respond(underkjentVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/iverksett") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Iverksetter vedtak for behandling $behandlingId")
                val vedtak = service.iverksattVedtak(behandlingId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK, vedtak.toDto())
            }
        }

        get("/loepende/{$SAKID_CALL_PARAMETER}") {
            withSakId(behandlingKlient) { sakId ->
                val dato =
                    call.request.queryParameters["dato"]?.let { LocalDate.parse(it) }
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

    route("/vedtak") {
        val logger = application.log

        get("/{$SAKID_CALL_PARAMETER}/behandlinger/nyeste/{resultat}") {
            val resultat: VedtakType = enumValueOf(requireNotNull(call.parameters["resultat"]))
            logger.info("Henter siste behandling med resultat $resultat")

            val nyeste = service.hentNyesteBehandlingMedResultat(sakId, resultat)
            if (nyeste != null) {
                call.respond(nyeste.toDto())
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.samordningsvedtakRoute(service: VedtaksvurderingService) {
    route("/api/samordning/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("samordning-read")
        }

        get {
            val virkFom =
                call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "virkFom ikke angitt")
            val fnr =
                call.request.headers["fnr"]?.let { Folkeregisteridentifikator.of(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val vedtaksliste = service.finnFerdigstilteVedtak(fnr, virkFom)
            call.respond(vedtaksliste.map { it.toSamordningsvedtakDto() })
        }

        get("/{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val vedtak = service.hentVedtak(vedtakId)
            if (vedtak != null) {
                call.respond(vedtak.toSamordningsvedtakDto())
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Vedtak.toVedtakSammendragDto() =
    VedtakSammendragDto(
        id = id.toString(),
        behandlingId = behandlingId,
        vedtakType = type,
        saksbehandlerId = vedtakFattet?.ansvarligSaksbehandler,
        datoFattet = vedtakFattet?.tidspunkt?.toNorskTid(),
        attestant = attestasjon?.attestant,
        datoAttestert = attestasjon?.tidspunkt?.toNorskTid(),
    )

private fun LoependeYtelse.toDto() =
    LoependeYtelseDTO(
        erLoepende = erLoepende,
        dato = dato,
    )

data class UnderkjennVedtakDto(val kommentar: String, val valgtBegrunnelse: String)

data class VedtakSammendragDto(
    val id: String,
    val behandlingId: UUID,
    val vedtakType: VedtakType?,
    val saksbehandlerId: String?,
    val datoFattet: ZonedDateTime?,
    val attestant: String?,
    val datoAttestert: ZonedDateTime?,
)

private fun Vedtak.toSamordningsvedtakDto() =
    VedtakSamordningDto(
        vedtakId = id,
        status = status,
        virkningstidspunkt = virkningstidspunkt,
        sak = VedtakSak(soeker.value, sakType, sakId),
        behandling = Behandling(behandlingType, behandlingId, revurderingAarsak, revurderingInfo),
        type = type,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon,
        beregning = beregning,
        avkorting = avkorting,
    )
