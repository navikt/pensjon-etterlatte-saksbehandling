package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
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
import java.time.YearMonth

class VedtakEtteroppgjoerService(
    private val repository: VedtaksvurderingRepository,
) {
    fun hentVedtakslisteIEtteroppgjoersAar(
        sakId: SakId,
        etteroppgjoersAar: Int,
    ): List<VedtakSamordningDto> {
        val vedtak = repository.hentVedtakForSak(sakId).firstOrNull()
        krevIkkeNull(vedtak) { "Fant ingen vedtak for sakId=$sakId" }

        val fnr = Folkeregisteridentifikator.of(vedtak.soeker.value)
        val vedtaksliste = repository.hentFerdigstilteVedtak(fnr, SakType.OMSTILLINGSSTOENAD)

        val tidslinjeJustert =
            Vedtakstidslinje(vedtaksliste)
                .sammenstill(YearMonth.of(etteroppgjoersAar, 1))
                .filter {
                    (it.innhold is VedtakInnhold.Behandling) &&
                        it.innhold.virkningstidspunkt.year == etteroppgjoersAar
                }

        val avkortetYtelsePerioderByVedtak =
            repository
                .hentAvkortetYtelsePerioder(tidslinjeJustert.map { it.id }.toSet())
                .groupBy { it.vedtakId }

        return tidslinjeJustert
            .map { it.toEtteroppgjoervedtakDto(avkortetYtelsePerioderByVedtak[it.id] ?: emptyList()) }
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
                .map { it.toEtteroppgjoerVedtakPeriode(innhold.utbetalingsperioder) },
    )
}

private fun AvkortetYtelsePeriode.toEtteroppgjoerVedtakPeriode(utbetalingsperioder: List<Utbetalingsperiode>): VedtakSamordningPeriode {
    val justertPeriode = utbetalingsperioder.firstOrNull { this.fom == it.periode.fom }

    return VedtakSamordningPeriode(
        fom = fom,
        tom = justertPeriode?.periode?.tom ?: tom,
        ytelseFoerAvkorting = ytelseFoerAvkorting,
        ytelseEtterAvkorting = ytelseEtterAvkorting,
    )
}
