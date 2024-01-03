package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import java.time.Clock
import java.time.Month
import java.time.YearMonth

val WAY_INTO_THE_FUTURE: YearMonth = YearMonth.of(2999, Month.DECEMBER)

internal fun erVedtakMedEtterbetaling(
    vedtakSomBehandles: Vedtak,
    vedtaksvurderingRepository: VedtaksvurderingRepository,
    clock: Clock = norskKlokke(),
): Boolean {
    when (val innhold = vedtakSomBehandles.innhold) {
        is VedtakBehandlingInnhold -> {
            val now = YearMonth.now(clock)

            if (innhold.virkningstidspunkt < now) {
                val ferdigstilteVedtak = vedtaksvurderingRepository.hentFerdigstilteVedtak(vedtakSomBehandles.soeker)
                val tidligereVedtakTidslinje = Vedtakstidslinje(ferdigstilteVedtak).sammenstill(YearMonth.of(2024, Month.JANUARY))
                val tidligereUtbetalingsperioder =
                    tidligereVedtakTidslinje
                        .filter { it.innhold is VedtakBehandlingInnhold }
                        .flatMap { (it.innhold as VedtakBehandlingInnhold).utbetalingsperioder }
                        .filter { it.periode.fom < now }

                // Finn tidligere utbetalingsperiode som matcher ny(e)
                // og sjekk om det er endring i beløp, hvis ny er høyere så er det etterbetaling
                return innhold.utbetalingsperioder
                    .filter { it.periode.fom < now }
                    .associateWith { tidligereUtbetalingsperioder.find { up -> up.overlapper(it) } }
                    .any { entry -> entry.value?.beloep?.compareTo(entry.key.beloep) == -1 }
            }
            return false
        }
        is VedtakTilbakekrevingInnhold -> throw IllegalArgumentException("Ikke aktuelt for tilbakekreving")
    }
}

internal fun Utbetalingsperiode.overlapper(other: Utbetalingsperiode): Boolean {
    return periode.overlapper(other.periode)
}

internal fun Periode.overlapper(other: Periode): Boolean {
    val thisTomSafe = this.tom ?: WAY_INTO_THE_FUTURE
    val thatTomSafe = other.tom ?: WAY_INTO_THE_FUTURE
    return this.fom == other.fom ||
        this.tom == other.tom ||
        other.fom.isBefore(thisTomSafe) && thatTomSafe.isAfter(this.fom)
}
