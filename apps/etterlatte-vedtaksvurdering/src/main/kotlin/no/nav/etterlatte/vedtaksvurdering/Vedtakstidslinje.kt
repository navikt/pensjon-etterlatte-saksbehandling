package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

class Vedtakstidslinje(
    private val vedtak: List<Vedtak>,
) {
    private val iverksatteVedtak = hentIverksatteVedtak()

    fun harLoependeVedtakPaaEllerEtter(dato: LocalDate): LoependeYtelse {
        val erUnderSamordning = vedtak.any { listOf(VedtakStatus.TIL_SAMORDNING, VedtakStatus.SAMORDNET).contains(it.status) }

        if (iverksatteVedtak.isEmpty()) return LoependeYtelse(false, erUnderSamordning, dato)

        val senesteVedtakPaaDato = hentSenesteVedtakSomKanLoepePaaDato(dato)
        val erLoepende = senesteVedtakPaaDato?.type in listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING)
        return LoependeYtelse(
            erLoepende = erLoepende,
            underSamordning = erUnderSamordning,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato,
            behandlingId = if (erLoepende) senesteVedtakPaaDato!!.behandlingId else null,
            sisteLoependeBehandlingId =
                if (erLoepende) {
                    sammenstill(YearMonth.from(dato))
                        .filter { it.type != VedtakType.OPPHOER }
                        .maxByOrNull { it.attestasjon?.tidspunkt!! }
                        ?.behandlingId
                } else {
                    null
                },
        )
    }

    private fun hentSenesteVedtakSomKanLoepePaaDato(dato: LocalDate): Vedtak? =
        iverksatteVedtak
            .filter { it.type.vanligBehandling }
            .filter {
                it.virkningstidspunkt
                    .atDay(1)
                    .isAfter(foersteMuligeVedtaksdag(dato))
                    .not()
            }.maxByOrNull { it.attestasjon?.tidspunkt!! }
            ?.let { senesteVedtak ->
                return if (senesteVedtak.opphoerFraOgMed == null || senesteVedtak.opphoerFraOgMed!!.atDay(1) > dato) {
                    senesteVedtak
                } else {
                    null
                }
            }

    private fun hentIverksatteVedtak(): List<Vedtak> = vedtak.filter { it.status === VedtakStatus.IVERKSATT }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato =
            (iverksatteVedtak.minBy { it.virkningstidspunkt }.innhold as VedtakInnhold.Behandling)
                .virkningstidspunkt
                .atDay(1)
        return maxOf(foersteVirkningsdato, fraDato)
    }

    /**
     * Opprette en kontinuerlig, "gjeldende" tidslinje med vedtak og underliggende perioder
     */
    fun sammenstill(fomDato: YearMonth): List<Vedtak> {
        val vedtakByVirkningsdato = mutableMapOf<Periode, Vedtak>()
        var currentVirkningstidspunkt: YearMonth? = null

        for (currentVedtak in vedtak
            .filter { it.innhold is VedtakInnhold.Behandling }
            .sortedByDescending { it.vedtakFattet!!.tidspunkt }) {
            with(currentVedtak) {
                if (currentVirkningstidspunkt?.isAfter(virkningstidspunkt) != false) {
                    val periode = Periode(virkningstidspunkt, currentVirkningstidspunkt?.minusMonths(1))
                    vedtakByVirkningsdato[periode] = currentVedtak
                    currentVirkningstidspunkt = virkningstidspunkt
                }
            }
        }

        return vedtakByVirkningsdato
            .filter { (periode, _) -> periode.tom == null || periode.tom == fomDato || periode.tom!!.isAfter(fomDato) }
            .map { (periode, vedtak) -> vedtak.kopier(periode) }
            .sortedBy { it.virkningstidspunkt }
    }

    // Opprette kopier av data class-struktur, med (potensielt) endret liste med utbetalingsperioder
    private fun Vedtak.kopier(gyldighetsperiode: Periode): Vedtak =
        copy(
            id = id,
            soeker = soeker,
            sakId = sakId,
            sakType = sakType,
            behandlingId = behandlingId,
            status = status,
            type = type,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
            innhold =
                when (innhold) {
                    is VedtakInnhold.Behandling -> innhold.kopier(gyldighetsperiode)
                    is VedtakInnhold.Tilbakekreving, is VedtakInnhold.Klage -> throw UgyldigForespoerselException(
                        code = "VEDTAKSINNHOLD_IKKE_STOETTET",
                        detail = "Skal ikke benyttes på annet enn vedtak med behandlingsinnhold",
                    )
                },
        )

    private fun VedtakInnhold.Behandling.kopier(gyldighetsperiode: Periode): VedtakInnhold.Behandling =
        copy(
            behandlingType = behandlingType,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            avkorting = avkorting,
            vilkaarsvurdering = vilkaarsvurdering,
            utbetalingsperioder = filtrerUtbetalingsperioder(gyldighetsperiode),
            revurderingInfo = revurderingInfo,
        )

    /**
     * 2 oppgaver:
     * - fjerne perioder som er utenfor vedtakets tidsrom (kan være endret pga revurdering tilbake i tid osv)
     * - lukke siste perioder dersom vedtakets gyldighetsperiode også er lukket, dvs det finnes vedtak med senere virkningstidspunkt
     */
    private fun VedtakInnhold.Behandling.filtrerUtbetalingsperioder(gyldighetsperiode: Periode): List<Utbetalingsperiode> =
        utbetalingsperioder
            .filter {
                gyldighetsperiode.tom?.let { tom -> !it.periode.fom.isAfter(tom) } ?: true
            }.map {
                if (it.periode.tom == null || gyldighetsperiode.tom?.isBefore(it.periode.tom) == true) {
                    it.copy(
                        id = it.id,
                        periode = Periode(it.periode.fom, gyldighetsperiode.tom),
                        beloep = it.beloep,
                        type = it.type,
                    )
                } else {
                    it
                }
            }

    private val Vedtak.virkningstidspunkt: YearMonth
        get() = (this.innhold as VedtakInnhold.Behandling).virkningstidspunkt

    private val Vedtak.opphoerFraOgMed: YearMonth?
        get() = (this.innhold as VedtakInnhold.Behandling).opphoerFraOgMed
}
