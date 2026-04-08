package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.behandling.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.behandling.vedtaksvurdering.OppdaterSamordningsmelding
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakBehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingRapidService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.behandling.vedtaksvurdering.toVedtakSammendragDto
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.AttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun Route.vedtaksvurderingRoute(
    vedtakService: VedtaksvurderingService,
    vedtakBehandlingService: VedtakBehandlingService,
    rapidService: VedtaksvurderingRapidService,
) {
    route("/api/vedtak") {
        val logger = LoggerFactory.getLogger("VedtaksvurderingRoute")

        get("/sak/{$SAKID_CALL_PARAMETER}/iverksatte") {
            logger.info("Henter iverksatte vedtak for sak $sakId")
            val iverksatteVedtak = inTransaction { vedtakService.hentIverksatteVedtakISak(sakId) }
            call.respond(iverksatteVedtak.map { it.toVedtakSammendragDto() })
        }

        get("/sak/{$SAKID_CALL_PARAMETER}/samordning") {
            logger.info("Henter samordningsinfo for sak $sakId")
            val samordningsinfo = inTransaction { runBlocking { vedtakBehandlingService.samordningsinfo(sakId) } }
            call.respond(samordningsinfo)
        }

        post("/sak/{$SAKID_CALL_PARAMETER}/samordning/melding") {
            val oppdatering = call.receive<OppdaterSamordningsmelding>()

            logger.info("Oppdaterer samordningsmelding=${oppdatering.samId}, sak=$sakId")
            inTransaction { runBlocking { vedtakBehandlingService.oppdaterSamordningsmelding(oppdatering, sakId, brukerTokenInfo) } }
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
            call.respond(HttpStatusCode.OK)
        }

        get("/sak/{$SAKID_CALL_PARAMETER}") {
            logger.info("Henter alle vedtak for sak $sakId")
            val vedtak = inTransaction { vedtakService.hentVedtakISak(sakId) }
            call.respond(vedtak.map { it.toVedtakSammendragDto() })
        }

        get("/sak/med-utbetaling/{inntektsaar}") {
            val inntektsaar =
                krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                    "Inntektsaar mangler"
                }
            call.respond(inTransaction { vedtakService.hentSakIdMedUtbetalingForInntektsaar(inntektsaar) })
        }

        get("/sak/{$SAKID_CALL_PARAMETER}/har-utbetaling-for-inntektsaar/{inntektsaar}") {
            val inntektsaar =
                krevIkkeNull(call.parameters["inntektsaar"]?.toInt()) {
                    "Inntektsaar mangler"
                }

            logger.info("Sjekker om sak $sakId har utbetaling for inntektsår $inntektsaar")
            val harUtbetaling = inTransaction { vedtakService.harSakUtbetalingForInntektsaar(sakId, inntektsaar) }
            call.respond(mapOf("harUtbetaling" to harUtbetaling))
        }

        get("/sak/{$SAKID_CALL_PARAMETER}/innvilgede-perioder") {
            val innvilgedePerioder =
                inTransaction {
                    vedtakService
                        .hentInnvilgedePerioder(sakId)
                        .map(InnvilgetPeriode::tilDto)
                }
            call.respond(innvilgedePerioder)
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/innvilgede-perioder") {
            val innvilgedePerioder =
                inTransaction {
                    vedtakService
                        .hentInnvilgedePerioder(behandlingId)
                        .map(InnvilgetPeriode::tilDto)
                }
            call.respond(innvilgedePerioder)
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            logger.info("Henter vedtak for behandling $behandlingId")
            val vedtak =
                inTransaction {
                    vedtakService.hentVedtakMedBehandlingId(behandlingId) ?: throw IkkeFunnetException(
                        "FANT_IKKE_VEDTAK",
                        "Fant ikke vedtaket til behandlingen med id=$behandlingId",
                    )
                }
            call.respond(vedtak.toDto())
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/sammendrag") {
            logger.info("Henter sammendrag av vedtak for behandling $behandlingId")
            val vedtaksresultat = inTransaction { vedtakService.hentVedtakMedBehandlingId(behandlingId)?.toVedtakSammendragDto() }
            if (vedtaksresultat == null) {
                call.response.status(HttpStatusCode.NoContent)
            } else {
                call.respond(vedtaksresultat)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/simulering") {
            kunSkrivetilgang {
                logger.info("Henter/oppdaterer vedtak for sinulering (behandling=$behandlingId)")

                val existingVedtak = inTransaction { vedtakService.hentVedtakMedBehandlingId(behandlingId) }
                existingVedtak?.let {
                    if (!it.underArbeid()) {
                        return@post call.respond(it.toDto())
                    }
                }

                val vedtak = inTransaction { vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo) }
                call.respond(vedtak.toDto())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/upsert") {
            kunSkrivetilgang {
                logger.info("Oppretter eller oppdaterer vedtak for behandling $behandlingId")
                val nyttVedtak = inTransaction { vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo) }
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/fattvedtak") {
            kunSkrivetilgang {
                logger.info("Fatter vedtak for behandling $behandlingId")
                val fattetVedtak = inTransaction { runBlocking { vedtakBehandlingService.fattVedtak(behandlingId, brukerTokenInfo) } }
                rapidService.sendToRapid(fattetVedtak)

                call.respond(fattetVedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/attester") {
            kunSkrivetilgang {
                logger.info("Attesterer vedtak for behandling $behandlingId")
                val (kommentar) = call.receive<AttesterVedtakDto>()
                val attestert =
                    inTransaction { runBlocking { vedtakBehandlingService.attesterVedtak(behandlingId, kommentar, brukerTokenInfo) } }
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
            val respons = inTransaction { runBlocking { vedtakBehandlingService.samordningsinfo(behandlingId) } }
            call.respond(respons)
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/underkjenn") {
            kunSkrivetilgang {
                logger.info("Underkjenner vedtak for behandling $behandlingId")
                val begrunnelse = call.receive<UnderkjennVedtakDto>()
                val underkjentVedtak =
                    inTransaction {
                        runBlocking {
                            vedtakBehandlingService.underkjennVedtak(
                                behandlingId,
                                brukerTokenInfo,
                                begrunnelse,
                            )
                        }
                    }
                rapidService.sendToRapid(underkjentVedtak)

                call.respond(underkjentVedtak.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/tilsamordning") {
            kunSkrivetilgang {
                logger.info("Vedtak er til samordning for behandling $behandlingId")
                val vedtak = inTransaction { vedtakBehandlingService.tilSamordningVedtak(behandlingId, brukerTokenInfo) }
                rapidService.sendToRapid(vedtak)
                call.respond(HttpStatusCode.OK, vedtak.rapidInfo1.vedtak)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/samordne") {
            kunSkrivetilgang {
                logger.info("Behandler samordning for behandling $behandlingId")
                val skalVentePaaSamordning =
                    inTransaction { runBlocking { vedtakBehandlingService.samordne(behandlingId, brukerTokenInfo) } }
                call.respond(HttpStatusCode.OK, mapOf("skalVentePaaSamordning" to skalVentePaaSamordning))
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/samordnet") {
            kunSkrivetilgang {
                logger.info("Vedtak ferdig samordning for behandling $behandlingId")
                val vedtak = inTransaction { vedtakBehandlingService.samordnetVedtak(behandlingId, brukerTokenInfo) }
                vedtak?.let { v ->
                    rapidService.sendToRapid(v)
                    call.respond(HttpStatusCode.OK, v.rapidInfo1.vedtak)
                } ?: call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/iverksett") {
            kunSkrivetilgang {
                logger.info("Iverksetter vedtak for behandling $behandlingId")
                val vedtak = inTransaction { vedtakBehandlingService.iverksattVedtak(behandlingId) }
                rapidService.sendToRapid(vedtak)

                call.respond(HttpStatusCode.OK, vedtak.vedtak)
            }
        }

        get("/loepende/{$SAKID_CALL_PARAMETER}") {
            val dato =
                call.request.queryParameters["dato"]?.let { LocalDate.parse(it) }
                    ?: throw Exception("dato er påkrevet på formatet YYYY-MM-DD")

            logger.info("Sjekker om sak har løpende for vedtak $sakId på dato $dato")
            val loependeYtelse = inTransaction { vedtakService.sjekkOmVedtakErLoependePaaDato(sakId, dato) }
            call.respond(loependeYtelse.toDto())
        }

        patch("/{$BEHANDLINGID_CALL_PARAMETER}/tilbakestill") {
            kunSkrivetilgang {
                logger.info("Tilbakestiller ikke iverksatte vedtak for behandling $behandlingId")
                inTransaction { vedtakBehandlingService.tilbakestillIkkeIverksatteVedtak(behandlingId) }
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/vedtak") {
        post("fnr") {
            val request = call.receive<Folkeregisteridentifikator>()
            val vedtak = inTransaction { vedtakService.hentVedtak(request).map { it.toDto() } }
            call.respond(vedtak)
        }

        route("/samordnet") {
            post("/{vedtakId}") {
                val vedtakId =
                    krevIkkeNull(call.parameters["vedtakId"]?.toLong()) {
                        "VedtakId mangler"
                    }

                val vedtak =
                    inTransaction {
                        vedtakService.hentVedtak(vedtakId)
                            ?: throw GenerellIkkeFunnetException()
                    }
                val samordnetVedtak = inTransaction { vedtakBehandlingService.samordnetVedtak(vedtak.behandlingId, brukerTokenInfo) }
                samordnetVedtak
                    ?.let { sv ->
                        rapidService.sendToRapid(sv)
                        call.respond(HttpStatusCode.OK, sv.rapidInfo1.vedtak)
                    } ?: call.respond(vedtak.toDto())
            }
        }
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
