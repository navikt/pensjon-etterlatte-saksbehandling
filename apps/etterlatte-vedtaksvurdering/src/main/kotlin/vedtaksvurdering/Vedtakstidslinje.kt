package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

class Vedtakstidslinje(private val vedtak: List<Vedtak>) {
    private val iverksatteVedtak = hentIverksatteVedtak()

    fun erLoependePaa(dato: LocalDate): LoependeYtelse {
        if (iverksatteVedtak.isEmpty()) return LoependeYtelse(false, dato)

        val erLoepende =
            hentSenesteVedtakPaaDato(dato)?.type in listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING)
        return LoependeYtelse(
            erLoepende = erLoepende,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato,
        )
    }

    private fun hentSenesteVedtakPaaDato(dato: LocalDate): Vedtak? =
        iverksatteVedtak
            .filter { (it.innhold as VedtakBehandlingInnhold).virkningstidspunkt.atDay(1).isAfter(foersteMuligeVedtaksdag(dato)).not() }
            .maxByOrNull { it.attestasjon?.tidspunkt!! }

    private fun hentIverksatteVedtak(): List<Vedtak> = vedtak.filter { it.status === VedtakStatus.IVERKSATT }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato =
            (
                iverksatteVedtak.minBy {
                    it.attestasjon?.tidspunkt ?: throw Error("Kunne ikke finne datoattestert paa vedtak")
                }.innhold as VedtakBehandlingInnhold
            ).virkningstidspunkt.atDay(1)
        return maxOf(foersteVirkningsdato, fraDato)
    }

    /**
     * Opprette en kontinuerlig, "gjeldende" tidslinje med vedtak og underliggende perioder
     */
    fun sammenstill(): List<Vedtak> {
        if (vedtak.size < 2) { // Unødvendig å gjøre noe
            return vedtak
        }

        val vedtakByVirkningsdato = mutableMapOf<Periode, Vedtak>()
        var currentVirkningstidspunkt: YearMonth? = null

        for (currentVedtak in vedtak
            .filter { it.innhold is VedtakBehandlingInnhold }
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
            .map { (k, v) -> v.kopier(k) }
            .sortedBy { it.virkningstidspunkt }
    }

    // Opprette kopier av data class-struktur, med (potensielt) endret liste med utbetalingsperioder
    private fun Vedtak.kopier(gyldighetsperiode: Periode): Vedtak {
        return copy(
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
                    is VedtakBehandlingInnhold -> innhold.kopier(gyldighetsperiode)
                    is VedtakTilbakekrevingInnhold -> throw UgyldigForespoerselException(
                        code = "VEDTAKSINNHOLD_IKKE_STOETTET",
                        detail = "Skal ikke benyttes på annet enn vedtak med behandlingsinnhold",
                    )
                },
        )
    }

    private fun VedtakBehandlingInnhold.kopier(gyldighetsperiode: Periode): VedtakBehandlingInnhold {
        return copy(
            behandlingType = behandlingType,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            avkorting = avkorting,
            vilkaarsvurdering = vilkaarsvurdering,
            utbetalingsperioder = filtrerUtbetalingsperioder(gyldighetsperiode),
            revurderingInfo = revurderingInfo,
        )
    }

    /**
     * 2 oppgaver:
     * - fjerne perioder som er utenfor vedtakets tidsrom (kan være endret pga revurdering tilbake i tid osv)
     * - lukke siste perioder dersom vedtakets gyldighetsperiode også er lukket, dvs det finnes vedtak med senere virkningstidspunkt
     */
    private fun VedtakBehandlingInnhold.filtrerUtbetalingsperioder(gyldighetsperiode: Periode): List<Utbetalingsperiode> {
        return utbetalingsperioder
            .filter {
                gyldighetsperiode.tom?.let { tom -> !it.periode.fom.isAfter(tom) } ?: true
            }
            .map {
                if (it.periode.tom == null || it.periode.tom?.isAfter(gyldighetsperiode.tom) == true) {
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
    }

    private val Vedtak.virkningstidspunkt: YearMonth
        get() = (this.innhold as VedtakBehandlingInnhold).virkningstidspunkt
}
