package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
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
        aar: Int,
    ): List<VedtakEtteroppgjoerDto> {
        val vedtak = repository.hentVedtakForSak(sakId).firstOrNull()
        krevIkkeNull(vedtak) { "Fant ingen vedtak for sakId=$sakId" }

        val fnr = Folkeregisteridentifikator.of(vedtak.soeker.value)
        val vedtaksliste = repository.hentFerdigstilteVedtak(fnr, SakType.OMSTILLINGSSTOENAD)

        val tidslinjeJustert =
            Vedtakstidslinje(vedtaksliste)
                .sammenstill(YearMonth.of(aar, 1))
                .filter {
                    (it.innhold is VedtakInnhold.Behandling) &&
                        it.innhold.virkningstidspunkt.year == aar
                }

        val avkortetYtelsePerioderByVedtak =
            repository
                .hentAvkortetYtelsePerioder(tidslinjeJustert.map { it.id }.toSet())
                .groupBy { it.vedtakId }

        return tidslinjeJustert
            .map { it.toEtteroppgjoervedtakDto(avkortetYtelsePerioderByVedtak[it.id] ?: emptyList()) }
    }
}

private fun Vedtak.toEtteroppgjoervedtakDto(avkortetYtelsePerioder: List<AvkortetYtelsePeriode>): VedtakEtteroppgjoerDto {
    val innhold = innhold as VedtakInnhold.Behandling

    return VedtakEtteroppgjoerDto(
        vedtakId = id,
        perioder =
            avkortetYtelsePerioder
                .map { it.toEtteroppgjoerVedtakPeriode(innhold.utbetalingsperioder) },
    )
}

private fun AvkortetYtelsePeriode.toEtteroppgjoerVedtakPeriode(utbetalingsperioder: List<Utbetalingsperiode>): VedtakEtteroppgjoerPeriode {
    val justertPeriode = utbetalingsperioder.firstOrNull { this.fom == it.periode.fom }

    return VedtakEtteroppgjoerPeriode(
        fom = fom,
        tom = justertPeriode?.periode?.tom ?: tom,
        ytelseEtterAvkorting = ytelseEtterAvkorting,
    )
}
