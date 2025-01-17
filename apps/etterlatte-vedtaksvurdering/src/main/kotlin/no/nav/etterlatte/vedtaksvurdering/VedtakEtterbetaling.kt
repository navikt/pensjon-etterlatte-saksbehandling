package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import java.math.BigDecimal
import java.time.Clock
import java.time.Month
import java.time.YearMonth

val WAY_INTO_THE_FUTURE: YearMonth = YearMonth.of(2999, Month.DECEMBER)

val OMS_START_YTELSE: YearMonth = YearMonth.of(2024, Month.JANUARY)

internal fun Vedtak.erVedtakMedEtterbetaling(
    vedtaksvurderingRepository: VedtaksvurderingRepository,
    grunnbeloep: Grunnbeloep,
    clock: Clock = norskKlokke(),
): EtterbetalingResultat {
    when (val innhold = this.innhold) {
        is VedtakInnhold.Behandling -> {
            val now = YearMonth.now(clock)

            if (innhold.virkningstidspunkt < now) {
                val ferdigstilteVedtak =
                    vedtaksvurderingRepository
                        .hentFerdigstilteVedtak(this.soeker, this.sakType)
                        .filter { it.id != this.id }
                val tidligereVedtakTidslinje = Vedtakstidslinje(ferdigstilteVedtak).sammenstill(OMS_START_YTELSE)
                val tidligereUtbetalingsperioder =
                    tidligereVedtakTidslinje
                        .filter { it.innhold is VedtakInnhold.Behandling }
                        .flatMap { (it.innhold as VedtakInnhold.Behandling).utbetalingsperioder }
                        .flatMap { it.flatUtPeriode() }
                        .filter { it.periode.fom < now }

                // Finn tidligere utbetalingsperiode som matcher ny(e)
                // og sjekk om det er endring i beløp, hvis ny er høyere så er det etterbetaling
                val perioderMedEtterbetaling =
                    innhold.utbetalingsperioder
                        .flatMap { it.flatUtPeriode() }
                        .filter { it.periode.fom < now }
                        .associateWith { nyPeriode -> tidligereUtbetalingsperioder.find { it.overlapper(nyPeriode) } }
                        .filter { (nyPeriode, tidligerePeriode) ->
                            tidligerePeriode?.beloepErMindreEnn(nyPeriode) ?: true
                        }

                return EtterbetalingResultat(
                    erEtterbetaling = perioderMedEtterbetaling.isNotEmpty(),
                    harUtvidetFrist = skalHaUtvidetFrist(perioderMedEtterbetaling, grunnbeloep),
                )
            }

            return EtterbetalingResultat.ingen()
        }

        is VedtakInnhold.Tilbakekreving, is VedtakInnhold.Klage -> throw IllegalArgumentException("Ikke aktuelt for tilbakekreving")
    }
}

private fun Utbetalingsperiode.beloepErMindreEnn(that: Utbetalingsperiode): Boolean =
    this.beloep.toNonNullBeloep() < that.beloep.toNonNullBeloep()

/**
 * Flater ut en gitt periode i flere perioder for å enklere sjekke etter overlapp
 * Eks. et objekt Periode(fom=2024-01, tom=2024-03) blir da 3 perioder, en for hver måned.
 **/
fun Utbetalingsperiode.flatUtPeriode(): List<Utbetalingsperiode> {
    val perioder = mutableListOf<Utbetalingsperiode>()
    val tom = this.periode.tom ?: YearMonth.now().minusMonths(1)

    var fom = this.periode.fom

    while (!fom.isAfter(tom)) {
        perioder.add(this.copy(periode = this.periode.copy(fom = fom, tom = fom)))
        fom = fom.plusMonths(1)
    }

    return perioder
}

/*
* Etterbetaling over 0.5G (en halv G), skal ha utvidet frist hos samordning
*/
private fun skalHaUtvidetFrist(
    perioderMedEtterbetaling: Map<Utbetalingsperiode, Utbetalingsperiode?>,
    grunnbeloep: Grunnbeloep,
): Boolean {
    if (perioderMedEtterbetaling.isEmpty()) return false

    val enHalvG = grunnbeloep.grunnbeloep.toBigDecimal().divide(BigDecimal.TWO)

    val totalForrigePerioder = perioderMedEtterbetaling.mapNotNull { it.value?.beloep }.sumOf { it }
    val totalNyePerioder = perioderMedEtterbetaling.mapNotNull { it.key.beloep }.sumOf { it }

    return (totalNyePerioder - totalForrigePerioder) > enHalvG
}

private fun BigDecimal?.toNonNullBeloep(): BigDecimal = this ?: BigDecimal.valueOf(0)

internal fun Utbetalingsperiode.overlapper(other: Utbetalingsperiode): Boolean = periode.overlapper(other.periode)

internal fun Periode.overlapper(other: Periode): Boolean {
    val thisTomSafe = this.tom ?: WAY_INTO_THE_FUTURE
    val thatTomSafe = other.tom ?: WAY_INTO_THE_FUTURE
    return this.fom == other.fom ||
        this.tom == other.tom ||
        other.fom.isBefore(thisTomSafe) &&
        thatTomSafe.isAfter(this.fom)
}

data class EtterbetalingResultat(
    val erEtterbetaling: Boolean,
    val harUtvidetFrist: Boolean = false,
) {
    companion object {
        fun ingen() = EtterbetalingResultat(erEtterbetaling = false, harUtvidetFrist = false)
    }
}
