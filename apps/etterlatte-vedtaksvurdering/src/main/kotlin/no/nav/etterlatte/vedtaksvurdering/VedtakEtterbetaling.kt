package no.nav.etterlatte.vedtaksvurdering

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
    clock: Clock = norskKlokke(),
): Boolean {
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
                        .filter { it.periode.fom < now }

                // Finn tidligere utbetalingsperiode som matcher ny(e)
                // og sjekk om det er endring i beløp, hvis ny er høyere så er det etterbetaling
                return innhold.utbetalingsperioder
                    .filter { it.periode.fom < now }
                    .associateWith { tidligereUtbetalingsperioder.find { up -> up.overlapper(it) } }
                    .any { entry -> entry.value?.beloepErMindreEnn(entry.key) ?: true }
            }
            return false
        }

        is VedtakInnhold.Tilbakekreving, is VedtakInnhold.Klage -> throw IllegalArgumentException("Ikke aktuelt for tilbakekreving")
    }
}

internal fun Utbetalingsperiode.beloepErMindreEnn(that: Utbetalingsperiode) = this.beloep?.compareTo(that.beloep.toNonNullBeloep()) == -1

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
