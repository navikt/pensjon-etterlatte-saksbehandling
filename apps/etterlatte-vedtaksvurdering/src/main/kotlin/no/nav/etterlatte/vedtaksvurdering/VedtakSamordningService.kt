package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
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
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVedtak(vedtakId: Long): VedtakSamordningDto? {
        logger.debug("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)?.let {
            val avkortetYtelsePerioder = repository.hentAvkortetYtelsePerioder(setOf(vedtakId))
            it.toSamordningsvedtakDto(avkortetYtelsePerioder)
        }
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

        val avkortetYtelsePerioderByVedtak =
            repository
                .hentAvkortetYtelsePerioder(tidslinjeJustert.map { it.id }.toSet())
                .groupBy { it.vedtakId }

        return tidslinjeJustert.map {
            val avkortetYtelsePerioder = avkortetYtelsePerioderByVedtak[it.id] ?: emptyList()
            it.toSamordningsvedtakDto(avkortetYtelsePerioder)
        }
    }
}

private fun Vedtak.toSamordningsvedtakDto(avkortetYtelsePerioder: List<AvkortetYtelsePeriode>): VedtakSamordningDto {
    val innhold = innhold as VedtakInnhold.Behandling

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
            ),
        virkningstidspunkt = innhold.virkningstidspunkt,
        beregning = innhold.beregning,
        perioder =
            avkortetYtelsePerioder
                // Vi har allerede fjernet overlappende utbetalingsperioder i tidslinjen for vedtak
                .filter { it.fom in innhold.utbetalingsperioder.map { it.periode.fom } }
                .map { it.toSamordningVedtakPeriode(innhold.utbetalingsperioder) },
    )
}

private fun AvkortetYtelsePeriode.toSamordningVedtakPeriode(utbetalingsperioder: List<Utbetalingsperiode>): VedtakSamordningPeriode {
    val justertPeriode = utbetalingsperioder.firstOrNull { this.fom == it.periode.fom }

    return VedtakSamordningPeriode(
        fom = fom,
        tom = justertPeriode?.periode?.tom ?: tom,
        ytelseFoerAvkorting = ytelseFoerAvkorting,
        ytelseEtterAvkorting = ytelseEtterAvkorting,
    )
}
