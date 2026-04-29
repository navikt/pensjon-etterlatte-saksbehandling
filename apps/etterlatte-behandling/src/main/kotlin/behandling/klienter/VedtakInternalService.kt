package no.nav.etterlatte.behandling.klienter

import no.nav.etterlatte.behandling.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.behandling.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakKlageService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakTilbakekrevingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.behandling.vedtaksvurdering.toVedtakSammendragDto
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class VedtakInternalService(
    private val vedtakTilbakekrevingService: VedtakTilbakekrevingService,
    private val vedtakKlageService: VedtakKlageService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) {
    private val logger = LoggerFactory.getLogger(VedtakInternalService::class.java)

    suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto {
        logger.info("Lagrer vedtak for tilbakekreving=${tilbakekrevingBehandling.id}")
        val vedtak =
            vedtakTilbakekrevingService.opprettEllerOppdaterVedtak(
                TilbakekrevingVedtakDto(
                    tilbakekrevingId = tilbakekrevingBehandling.id,
                    sakId = tilbakekrevingBehandling.sak.id,
                    sakType = tilbakekrevingBehandling.sak.sakType,
                    soeker = Folkeregisteridentifikator.of(tilbakekrevingBehandling.sak.ident),
                    tilbakekreving = tilbakekrevingBehandling.tilbakekreving.toObjectNode(),
                ),
            )
        return vedtak.toDto()
    }

    suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto {
        logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingId")
        val vedtak =
            vedtakTilbakekrevingService.fattVedtak(
                TilbakekrevingFattEllerAttesterVedtakDto(
                    tilbakekrevingId = tilbakekrevingId,
                    enhet = enhet,
                ),
                brukerTokenInfo,
            )
        return vedtak.toDto()
    }

    suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto {
        logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingId")
        val vedtak =
            vedtakTilbakekrevingService.attesterVedtak(
                TilbakekrevingFattEllerAttesterVedtakDto(
                    tilbakekrevingId = tilbakekrevingId,
                    enhet = enhet,
                ),
                brukerTokenInfo,
            )
        return vedtak.toDto()
    }

    suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        val vedtak = vedtakTilbakekrevingService.underkjennVedtak(tilbakekrevingId)
        return vedtak.toDto()
    }

    suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        logger.info("Lagrer vedtak for klage=${klage.id}")
        val vedtak = vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        return vedtak.toDto()
    }

    suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        logger.info("Fatter vedtak for klage=${klage.id}")
        val vedtak = vedtakKlageService.fattVedtak(klage, brukerTokenInfo)
        return vedtak.toDto()
    }

    suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        logger.info("Attesterer vedtak for klage=${klage.id}")
        val vedtak = vedtakKlageService.attesterVedtak(klage, brukerTokenInfo)
        return vedtak.toDto()
    }

    suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        logger.info("Underkjenner vedtak for klage=$klageId")
        val vedtak = vedtakKlageService.underkjennVedtak(klageId)
        return vedtak.toDto()
    }

    suspend fun sakHarLopendeVedtakPaaDato(
        sakId: SakId,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO {
        logger.info("Sjekker om sak $sakId er løpende på $dato")
        val loependeYtelse = vedtaksvurderingService.sjekkOmVedtakErLoependePaaDato(sakId, dato)
        return loependeYtelse.toDto()
    }

    suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto? {
        logger.info("Henter vedtak for behandling=$behandlingId")
        return vedtaksvurderingService.hentVedtakMedBehandlingId(behandlingId)?.toDto()
    }

    suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> {
        logger.info("Henter iverksatte vedtak for sak=$sakId")
        return vedtaksvurderingService
            .hentIverksatteVedtakISak(sakId)
            .map { it.toVedtakSammendragDto() }
    }

    suspend fun hentSakerMedUtbetalingForInntektsaar(
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<SakId> {
        logger.info("Henter saker med utbetaling for inntektsår=$inntektsaar")
        return vedtaksvurderingService.hentSakIdMedUtbetalingForInntektsaar(inntektsaar)
    }

    suspend fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Sjekker om sak $sakId har utbetaling for inntektsår $inntektsaar")
        return vedtaksvurderingService.harSakUtbetalingForInntektsaar(sakId, inntektsaar)
    }

    suspend fun hentInnvilgedePerioder(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<InnvilgetPeriodeDto> {
        logger.info("Henter innvilgede perioder for sak=$sakId")
        return vedtaksvurderingService
            .hentInnvilgedePerioder(sakId)
            .map(InnvilgetPeriode::tilDto)
    }

    suspend fun angreAttesteringTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: Saksbehandler,
        enhet: Enhetsnummer,
    ): VedtakDto {
        logger.info("Angrer attestering for tilbakekreving=$tilbakekrevingId")
        val vedtak = vedtakTilbakekrevingService.tilbakeStillAttestert(tilbakekrevingId)
        return vedtak.toDto()
    }

    private fun LoependeYtelse.toDto(): LoependeYtelseDTO =
        LoependeYtelseDTO(
            erLoepende = this.erLoepende,
            underSamordning = this.underSamordning,
            dato = this.dato,
            behandlingId = this.behandlingId,
            sisteLoependeBehandlingId = this.sisteLoependeBehandlingId,
        )
}

class VedtakKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
