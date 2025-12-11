package no.nav.etterlatte.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakEtteroppgjoerService
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun Route.vedtaksvurderingRoute(
    vedtakService: VedtaksvurderingService,
    vedtakBehandlingService: VedtakBehandlingService,
    rapidService: VedtaksvurderingRapidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = LoggerFactory.getLogger("VedtaksvurderingRoute")

        get("/sak/{$SAKID_CALL_PARAMETER}/iverksatte") {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter iverksatte vedtak for sak $sakId")
                val iverksatteVedtak = vedtakBehandlingService.hentIverksatteVedtakISak(sakId)
                call.respond(iverksatteVedtak.map { it.toVedtakSammendragDto() })
            }
        }

        get("/sak/{$SAKID_CALL_PARAMETER}/samordning") {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter samordningsinfo for sak $sakId")
                val samordningsinfo = vedtakBehandlingService.samordningsinfo(sakId)
                call.respond(samordningsinfo)
            }
        }

        post("/sak/{$SAKID_CALL_PARAMETER}/samordning/melding") {
            withSakId(behandlingKlient) { sakId ->
                val oppdatering = call.receive<OppdaterSamordningsmelding>()

                logger.info("Oppdaterer samordningsmelding=${oppdatering.samId}, sak=$sakId")
                vedtakBehandlingService.oppdaterSamordningsmelding(oppdatering, brukerTokenInfo).run {
                    rapidService.sendGenerellHendelse(
                        VedtakKafkaHendelseHendelseType.SAMORDNING_MANUELT_BEHANDLET,
                        mapOf(
                            "sakId" to sakId,
                            "vedtakId" to oppdatering.vedtakId,
                            "samordningsmeldingId" to oppdatering.samId,
                            "saksbehandlerId" to brukerTokenInfo.ident(),
                            "kommentar" to oppdatering.kommentar,
                        ),
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            withSakId(behandlingKlient) { sakId ->
                logger.info("Henter alle vedtak for sak $sakId")
                val vedtak = vedtakService.hentVedtakISak(sakId)
                call.respond(vedtak.map { it.toVedtakSammendragDto() })
            }
        }

        get("/sak/med-utbetaling/{inntektsaar}") {
            val inntektsaar =
                krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                    "Inntektsaar mangler"
                }

            call.respond(vedtakService.hentSakIdMedUtbetalingForInntektsaar(inntektsaar))
        }

        get("/sak/{$SAKID_CALL_PARAMETER}/innvilgede-perioder") {
            val innvilgedePerioder =
                vedtakService
                    .hentInnvilgedePerioder(sakId)
                    .map(InnvilgetPeriode::tilDto)
            call.respond(innvilgedePerioder)
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter vedtak for behandling $behandlingId")
                val vedtak =
                    vedtakService.hentVedtakMedBehandlingId(behandlingId) ?: throw IkkeFunnetException(
                        "FANT_IKKE_VEDTAK",
                        "Fant ikke vedtaket til behandlingen med id=$behandlingId",
                    )
                call.respond(vedtak.toDto())
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

        post("/{$BEHANDLINGID_CALL_PARAMETER}/simulering") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Henter/oppdaterer vedtak for sinulering (behandling=$behandlingId)")

                vedtakService.hentVedtakMedBehandlingId(behandlingId)?.let {
                    if (!it.underArbeid()) {
                        return@post call.respond(it.toDto())
                    }
                }

                val vedtak =
                    vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
                call.respond(vedtak.toDto())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/upsert") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Oppretter eller oppdaterer vedtak for behandling $behandlingId")
                val nyttVedtak = vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/fattvedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Fatter vedtak for behandling $behandlingId")
                val fattetVedtak = vedtakBehandlingService.fattVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(fattetVedtak)

                call.respond(fattetVedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/attester") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Attesterer vedtak for behandling $behandlingId")
                val (kommentar) = call.receive<AttesterVedtakDto>()
                val attestert = vedtakBehandlingService.attesterVedtak(behandlingId, kommentar, brukerTokenInfo)

                try {
                    rapidService.sendToRapid(attestert)
                } catch (e: Exception) {
                    logger.error(
                        "Kan ikke sende attestert vedtak på kafka for behandling id: $behandlingId, vedtak: ${attestert.vedtak.id} " +
                            "Saknr: ${attestert.vedtak.sak.id}. " +
                            "Det betyr at vi ikke får sendt ut vedtaksbrev og heller ikke utbetalingsoppdrag. " +
                            "Denne hendelsen må sendes ut manuelt straks.",
                        e,
                    )
                    throw e
                }
                call.respond(attestert.vedtak)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/samordning") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val respons = vedtakBehandlingService.samordningsinfo(behandlingId)
                call.respond(respons)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/underkjenn") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
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
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Vedtak er til samordning for behandling $behandlingId")
                val vedtak = vedtakBehandlingService.tilSamordningVedtak(behandlingId, brukerTokenInfo)
                rapidService.sendToRapid(vedtak)
                call.respond(HttpStatusCode.OK, vedtak.rapidInfo1.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/samordne") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Behandler samordning for behandling $behandlingId")
                val skalVentePaaSamordning = vedtakBehandlingService.samordne(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK, mapOf("skalVentePaaSamordning" to skalVentePaaSamordning))
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/samordnet") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Vedtak ferdig samordning for behandling $behandlingId")

                vedtakBehandlingService.samordnetVedtak(behandlingId, brukerTokenInfo)?.let { vedtak ->
                    rapidService.sendToRapid(vedtak)
                    call.respond(HttpStatusCode.OK, vedtak.rapidInfo1.vedtak)
                } ?: call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/iverksett") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
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

                logger.info("Sjekker om sak har løpende for vedtak $sakId på dato $dato")
                val loependeYtelse = vedtakBehandlingService.sjekkOmVedtakErLoependePaaDato(sakId, dato)
                call.respond(loependeYtelse.toDto())
            }
        }

        patch("/{$BEHANDLINGID_CALL_PARAMETER}/tilbakestill") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Tilbakestiller ikke iverksatte vedtak for behandling $behandlingId")
                vedtakBehandlingService.tilbakestillIkkeIverksatteVedtak(behandlingId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/vedtak") {
        post("fnr") {
            val request = call.receive<Folkeregisteridentifikator>()
            val vedtak = vedtakService.hentVedtak(request).map { it.toDto() }
            call.respond(vedtak)
        }

        route("/samordnet") {
            post("/{vedtakId}") {
                val vedtakId =
                    krevIkkeNull(call.parameters["vedtakId"]?.toLong()) {
                        "VedtakId mangler"
                    }

                val vedtak =
                    vedtakService.hentVedtak(vedtakId)
                        ?: throw GenerellIkkeFunnetException()

                vedtakBehandlingService
                    .samordnetVedtak(vedtak.behandlingId, brukerTokenInfo)
                    ?.let { samordnetVedtak ->
                        rapidService.sendToRapid(samordnetVedtak)
                        call.respond(HttpStatusCode.OK, samordnetVedtak.rapidInfo1.vedtak)
                    } ?: call.respond(vedtak.toDto())
            }
        }
    }
}

fun Route.samordningSystembrukerVedtakRoute(vedtakSamordningService: VedtakSamordningService) {
    route("/api/samordning/vedtak") {
        post {
            val sakstype =
                call.parameters["sakstype"]?.let { runCatching { SakType.valueOf(it) }.getOrNull() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "sakstype ikke angitt")
            val fomDato =
                call.parameters["fomDato"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "fomDato ikke angitt")
            val fnr = call.receive<FoedselsnummerDTO>().foedselsnummer.let { Folkeregisteridentifikator.of(it) }

            val vedtaksliste =
                vedtakSamordningService.hentVedtaksliste(
                    fnr = fnr,
                    sakType = sakstype,
                    fomDato = fomDato,
                )
            call.respond(vedtaksliste)
        }

        get("/{vedtakId}") {
            val vedtakId =
                krevIkkeNull(call.parameters["vedtakId"]?.toLong()) {
                    "VedtakId mangler"
                }

            val vedtak =
                vedtakSamordningService.hentVedtak(vedtakId)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(vedtak)
        }
    }
}

fun Route.etteroppgjoerSystembrukerVedtakRoute(
    vedtakEtteroppgjoerService: VedtakEtteroppgjoerService,
    behandlingKlient: BehandlingKlient,
) {
    route("/vedtak/etteroppgjoer/{$SAKID_CALL_PARAMETER}") {
        post {
            withSakId(behandlingKlient, skrivetilgang = true) {
                val request = call.receive<VedtakslisteEtteroppgjoerRequest>()

                val vedtaksliste =
                    vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(
                        sakId = request.sakId,
                        aar = request.etteroppgjoersAar,
                    )
                call.respond(vedtaksliste)
            }
        }
    }
}

fun Route.tilbakekrevingvedtakRoute(
    service: VedtakTilbakekrevingService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = LoggerFactory.getLogger("TilbakekrevingsvedtakRoute")
    route("/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/lagre-vedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                val dto = call.receive<TilbakekrevingVedtakDto>()
                logger.info("Oppretter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.opprettEllerOppdaterVedtak(dto).toDto())
            }
        }
        post("/fatt-vedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Fatter vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.fattVedtak(dto, brukerTokenInfo).toDto())
            }
        }
        post("/attester-vedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                val dto = call.receive<TilbakekrevingFattEllerAttesterVedtakDto>()
                logger.info("Attesterer vedtak for tilbakekreving=${dto.tilbakekrevingId}")
                call.respond(service.attesterVedtak(dto, brukerTokenInfo).toDto())
            }
        }
        post("/underkjenn-vedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Underkjenner vedtak for tilbakekreving=$behandlingId")
                call.respond(service.underkjennVedtak(behandlingId).toDto())
            }
        }
        post("/tilbakestill-vedtak") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Tilbakestiller vedtak fra attestert for tilbakekreving med id=$behandlingId")
                call.respond(service.tilbakeStillAttestert(behandlingId).toDto())
            }
        }
    }
}

fun Route.klagevedtakRoute(
    service: VedtakKlageService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = LoggerFactory.getLogger("KlagevedtakRoute")

    route("/vedtak/klage/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/upsert") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")
                logger.info("Oppretter vedtak for klage med id=$behandlingId")

                call.respond(service.opprettEllerOppdaterVedtakOmAvvisning(klage).toDto())
            }
        }

        post("/fatt") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")

                logger.info("Fatter vedtak for klage med id=$behandlingId")
                call.respond(service.fattVedtak(klage, brukerTokenInfo).toDto())
            }
        }

        post("/attester") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                val klage = call.receive<Klage>()
                if (klage.id != behandlingId) throw MismatchingIdException("Klage-ID i path og i request body er ikke like")

                logger.info("Attesterer vedtak for klage med id=$behandlingId")
                call.respond(service.attesterVedtak(klage, brukerTokenInfo).toDto())
            }
        }
        post("/underkjenn") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Underkjenner vedtak for klage=$behandlingId")
                call.respond(service.underkjennVedtak(behandlingId).toDto())
            }
        }
    }
}

private fun Vedtak.toVedtakSammendragDto(): VedtakSammendragDto {
    val dto =
        VedtakSammendragDto(
            id = id.toString(),
            behandlingId = behandlingId,
            vedtakType = type,
            behandlendeSaksbehandler = vedtakFattet?.ansvarligSaksbehandler,
            datoFattet = vedtakFattet?.tidspunkt?.toNorskTid(),
            attesterendeSaksbehandler = attestasjon?.attestant,
            datoAttestert = attestasjon?.tidspunkt?.toNorskTid(),
            virkningstidspunkt = null,
            opphoerFraOgMed = null,
            iverksettelsesTidspunkt = iverksettelsesTidspunkt,
        )
    return when (innhold) {
        is VedtakInnhold.Behandling ->
            dto.copy(
                virkningstidspunkt = innhold.virkningstidspunkt,
                opphoerFraOgMed = innhold.opphoerFraOgMed,
            )

        else -> dto
    }
}

private fun LoependeYtelse.toDto() =
    LoependeYtelseDTO(
        erLoepende = erLoepende,
        underSamordning = underSamordning,
        dato = dato,
        behandlingId = behandlingId,
        sisteLoependeBehandlingId = sisteLoependeBehandlingId,
    )

data class UnderkjennVedtakDto(
    val kommentar: String,
    val valgtBegrunnelse: String,
)

private class MismatchingIdException(
    message: String,
) : ForespoerselException(
        HttpStatusCode.BadRequest.value,
        "ID_MISMATCH_MELLOM_PATH_OG_BODY",
        message,
    )

data class VedtakslisteEtteroppgjoerRequest(
    val sakId: SakId,
    val etteroppgjoersAar: Int,
)
