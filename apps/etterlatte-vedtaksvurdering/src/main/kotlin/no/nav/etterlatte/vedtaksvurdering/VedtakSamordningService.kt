package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class VedtakSamordningService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakSamordningService::class.java)

    fun hentVedtak(vedtakId: Long): VedtakSamordningDto? {
        logger.debug("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)?.toSamordningsvedtakDto()
    }

    fun hentVedtaksliste(
        fnr: Folkeregisteridentifikator,
        sakType: SakType,
        fomDato: LocalDate,
    ): List<VedtakSamordningDto> {
        logger.debug("Henter og sammenstiller vedtaksliste")
        val vedtaksliste = repository.hentFerdigstilteVedtak(fnr, sakType)
        val tidslinjeJustert =
            Vedtakstidslinje(vedtaksliste)
                .sammenstill(YearMonth.of(fomDato.year, fomDato.month))
        return tidslinjeJustert.map { it.toSamordningsvedtakDto() }
    }
}

private fun Vedtak.toSamordningsvedtakDto(): VedtakSamordningDto {
    val innhold = innhold as VedtakInnhold.Behandling
    val avkorting = innhold.avkorting?.let { deserialize<AvkortingDto>(it.toString()) }

    return VedtakSamordningDto(
        vedtakId = id,
        fnr = soeker.value,
        status = status,
        sak = VedtakSak(soeker.value, sakType, sakId),
        type = type,
        vedtakFattet = vedtakFattet,
        attestasjon = attestasjon,
        behandling =
            Behandling(
                innhold.behandlingType,
                behandlingId,
                innhold.revurderingAarsak,
                innhold.revurderingInfo,
            ),
        virkningstidspunkt = innhold.virkningstidspunkt,
        beregning = innhold.beregning,
        perioder =
            avkorting
                ?.avkortetYtelse
                ?.map { it.toSamordningVedtakPeriode(innhold.utbetalingsperioder) }
                ?: emptyList(),
    )
}

private fun AvkortetYtelseDto.toSamordningVedtakPeriode(utbetalingsperioder: List<Utbetalingsperiode>): VedtakSamordningPeriode {
    val justertPeriode = utbetalingsperioder.firstOrNull { this.fom == it.periode.fom }

    return VedtakSamordningPeriode(
        fom = fom,
        tom = justertPeriode?.periode?.tom ?: tom,
        ytelseFoerAvkorting = ytelseFoerAvkorting,
        ytelseEtterAvkorting = ytelseEtterAvkorting,
    )
}
