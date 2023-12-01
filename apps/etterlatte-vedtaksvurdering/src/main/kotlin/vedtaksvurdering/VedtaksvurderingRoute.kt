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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import java.time.LocalDate

fun Route.vedtaksvurderingRoute(
    vedtakService: VedtaksvurderingService,
    vedtakBehandlingService: VedtakBehandlingService,
    rapidService: VedtaksvurderingRapidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = application.log

        get("/sak/{${SAKID_CALL_PARAMETER}}/iverksatte") {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter iverksatte vedtak for sak $sakId")
                val iverksatteVedtak = vedtakBehandlingService.hentIverksatteVedtakISak(sakId)
                call.respond(iverksatteVedtak.map { it.toVedtakSammendragDto() })
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter vedtak for behandling $behandlingId")
                val vedtak = vedtakService.hentVedtakMedBehandlingId(behandlingId)
                if (vedtak == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtak.toDto())
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/ny") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter vedtak for behandling $behandlingId")
                val vedtak = vedtakService.hentVedtakMedBehandlingId(behandlingId)
                if (vedtak == null) {
                    call.response.status(HttpStatusCode.NotFound)
                } else {
                    call.respond(vedtak.toNyDto())
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/sammendrag") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter sammendrag av vedtak for behandling $behandlingId")
                val vedtaksresultat = vedtakService.hentVedtakMedBehandlingId(behandlingId)?.toVedtakSammendragDto()
                if (vedtaksresultat == null) {
                    call.response.status(HttpStatusCode.NoContent)
                } else {
                    call.respond(vedtaksresultat)
                }
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/upsert") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Oppretter eller oppdaterer vedtak for behandling $behandlingId")
                val nyttVedtak = vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/fattvedtak") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Fatter vedtak for behandling $behandlingId")
                val fattetVedtak = vedtakBehandlingService.fattVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(fattetVedtak)

                call.respond(fattetVedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/attester") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Attesterer vedtak for behandling $behandlingId")
                val (kommentar) = call.receive<AttesterVedtakDto>()
                val attestert = vedtakBehandlingService.attesterVedtak(behandlingId, kommentar, brukerTokenInfo)

                try {
                    rapidService.sendToRapid(attestert)
                } catch (e: Exception) {
                    logger.error(
                        "Kan ikke sende attestert vedtak på kafka for behandling id: $behandlingId, vedtak: ${attestert.vedtak.vedtakId} " +
                            "Saknr: ${attestert.vedtak.sak.id}. " +
                            "Det betyr at vi ikke sender ut brev for vedtaket eller at en utbetaling går til oppdrag. " +
                            "Denne hendelsen må sendes ut manuelt straks.",
                        e,
                    )
                    throw e
                }
                call.respond(attestert.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/underkjenn") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Underkjenner vedtak for behandling $behandlingId")
                val begrunnelse = call.receive<UnderkjennVedtakDto>()
                val underkjentVedtak =
                    vedtakBehandlingService.underkjennVedtak(
                        behandlingId,
                        brukerTokenInfo,
                        begrunnelse,
                    )
                rapidService.sendToRapid(underkjentVedtak)

                call.respond(underkjentVedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/tilsamordning") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Vedtak er til samordning for behandling $behandlingId")
                val vedtak = vedtakBehandlingService.tilSamordningVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(vedtak)
                call.respond(HttpStatusCode.OK, vedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/samordnet") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Vedtak ferdig samordning for behandling $behandlingId")
                val vedtak = vedtakBehandlingService.samordnetVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(vedtak)
                call.respond(HttpStatusCode.OK, vedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/iverksett") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Iverksetter vedtak for behandling $behandlingId")
                val vedtak = vedtakBehandlingService.iverksattVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(vedtak)

                call.respond(HttpStatusCode.OK, vedtak.vedtak)
            }
        }

        get("/loepende/{$SAKID_CALL_PARAMETER}") {
            withSakId(behandlingKlient) { sakId ->
                val dato =
                    call.request.queryParameters["dato"]?.let { LocalDate.parse(it) }
                        ?: throw Exception("dato er påkrevet på formatet YYYY-MM-DD")

                logger.info("Sjekker om vedtak er løpende for sak $sakId på dato $dato")
                val loependeYtelse = vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(sakId, dato)
                call.respond(loependeYtelse.toDto())
            }
        }

        patch("/{$BEHANDLINGID_CALL_PARAMETER}/tilbakestill") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Tilbakestiller ikke iverksatte vedtak for behandling $behandlingId")
                vedtakBehandlingService.tilbakestillIkkeIverksatteVedtak(behandlingId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/vedtak") {
        val logger = application.log

        get("/{$SAKID_CALL_PARAMETER}/behandlinger/nyeste/{resultat}") {
            val resultat: VedtakType = enumValueOf(requireNotNull(call.parameters["resultat"]))
            logger.info("Henter siste behandling med resultat $resultat")

            val nyeste = vedtakBehandlingService.hentNyesteBehandlingMedResultat(sakId, resultat)
            if (nyeste != null) {
                call.respond(nyeste.toDto())
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        route("/samordnet") {
            install(AuthorizationPlugin) {
                roles = setOf("samordning-write")
            }

            post("/{vedtakId}") {
                val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

                val vedtak = vedtakService.hentVedtak(vedtakId)
                if (vedtak == null) {
                    call.respond(HttpStatusCode.NotFound)
                }

                val samordnetVedtak = vedtakBehandlingService.samordnetVedtak(vedtak!!.behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(samordnetVedtak)
                call.respond(HttpStatusCode.OK, samordnetVedtak.vedtak)
            }
        }
    }
}

fun Route.samordningsvedtakRoute(vedtakSamordningService: VedtakSamordningService) {
    route("/api/samordning/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("samordning-read")
        }

        get {
            val fomDato =
                call.parameters["fomDato"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fomDato ikke angitt")
            val fnr =
                call.request.headers["fnr"]?.let { Folkeregisteridentifikator.of(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val vedtaksliste = vedtakSamordningService.hentVedtaksliste(fnr, fomDato)
            call.respond(vedtaksliste)
        }

        get("/{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val vedtak = vedtakSamordningService.hentVedtak(vedtakId)
            if (vedtak != null) {
                call.respond(vedtak)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun Route.tilbakekrevingvedtakRoute(
    service: VedtakTilbakekrevingService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = application.log

    route("/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/lagre-vedtak") {
            withBehandlingId(behandlingKlient) {
                val dto = call.receive<TilbakekrevingVedtakDto>()
                logger.info("Oppretter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.opprettEllerOppdaterVedtak(dto))
            }
        }
        post("/fatt-vedtak") {
            withBehandlingId(behandlingKlient) {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Fatter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.fattVedtak(dto))
            }
        }
        post("/attester-vedtak") {
            withBehandlingId(behandlingKlient) {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Attesterer vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.attesterVedtak(dto))
            }
        }
        post("/underkjenn-vedtak") {
            withBehandlingId(behandlingKlient) {
                logger.info("Underkjenner vedtak for tilbakekreving=$behandlingId")
                call.respond(service.underkjennVedtak(behandlingId))
            }
        }
    }
}

private fun Vedtak.toVedtakSammendragDto() =
    VedtakSammendragDto(
        id = id.toString(),
        behandlingId = behandlingId,
        vedtakType = type,
        behandlendeSaksbehandler = vedtakFattet?.ansvarligSaksbehandler,
        datoFattet = vedtakFattet?.tidspunkt?.toNorskTid(),
        attesterendeSaksbehandler = attestasjon?.attestant,
        datoAttestert = attestasjon?.tidspunkt?.toNorskTid(),
    )

private fun LoependeYtelse.toDto() =
    LoependeYtelseDTO(
        erLoepende = erLoepende,
        dato = dato,
    )

data class UnderkjennVedtakDto(val kommentar: String, val valgtBegrunnelse: String)
