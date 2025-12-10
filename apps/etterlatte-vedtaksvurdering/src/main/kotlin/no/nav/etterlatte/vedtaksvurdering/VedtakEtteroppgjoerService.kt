package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.Vedtakstidslinje
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class VedtakEtteroppgjoerService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVedtaksliste(
        fnr: Folkeregisteridentifikator,
        etteroppgjoersAar: Int,
    ): List<VedtakSamordningDto> {
        val fomDato = LocalDate.of(etteroppgjoersAar, 1, 1)

        val vedtaksliste = repository.hentFerdigstilteVedtak(fnr, SakType.OMSTILLINGSSTOENAD)
        val tidslinjeJustert =
            Vedtakstidslinje(vedtaksliste)
                .sammenstill(YearMonth.of(fomDato.year, fomDato.month))

        val avkortetYtelsePerioderByVedtak =
            repository
                .hentAvkortetYtelsePerioder(tidslinjeJustert.map { it.id }.toSet())
                .groupBy { it.vedtakId }

        return tidslinjeJustert.map {
            val avkortetYtelsePerioder = avkortetYtelsePerioderByVedtak[it.id] ?: emptyList()
            it.toEtteroppgjoervedtakDto(avkortetYtelsePerioder)
        }
    }
}

private fun Vedtak.toEtteroppgjoervedtakDto(avkortetYtelsePerioder: List<AvkortetYtelsePeriode>): VedtakSamordningDto {
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
