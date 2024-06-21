package no.nav.etterlatte.utbetaling.avstemming.vedtak

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingsperiodeType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import java.math.BigDecimal
import java.time.YearMonth

class Vedtaksverifiserer {
    fun verifiser(
        utbetaling: Utbetaling,
        vedtak: Utbetalingsvedtak,
    ) {
        (vedtak.pensjonTilUtbetaling).forEach { fraVedtak ->
            val korresponderendeUtbetalingslinje =
                utbetaling.utbetalingslinjer
                    .maxByOrNull { YearMonth.from(it.periode.fra) == fraVedtak.periode.fom }
                    ?: throw IllegalStateException(
                        "Mangler korresponderende utbetalingslinje for periode ${fraVedtak.periode} " +
                            "for vedtak ${vedtak.vedtakId}",
                    )
            verifiserAtStemmerOverens(vedtak.vedtakId, fraVedtak, korresponderendeUtbetalingslinje)
        }
    }

    private fun verifiserAtStemmerOverens(
        vedtakId: Long,
        fraVedtak: Utbetalingsperiode,
        korresponderendeUtbetalingslinje: Utbetalingslinje,
    ) {
        check(sammenlignBeloep(fraVedtak, korresponderendeUtbetalingslinje)) {
            "Beløp fra vedtak $vedtakId var ${fraVedtak.beloep}, men i utbetalingslinje ${korresponderendeUtbetalingslinje.beloep}"
        }
        check(fraVedtak.periode.tom == korresponderendeUtbetalingslinje.periode.til?.let { YearMonth.from(it) }) {
            "Tom-periode fra vedtak $vedtakId var ${fraVedtak.periode.tom}, men i utbetalingslinje ${
                korresponderendeUtbetalingslinje.periode.til?.let {
                    YearMonth.from(
                        it,
                    )
                }
            }"
        }
    }

    private fun sammenlignBeloep(
        fraVedtak: Utbetalingsperiode,
        korresponderendeUtbetalingslinje: Utbetalingslinje,
    ): Boolean {
        if (fraVedtak.type == UtbetalingsperiodeType.OPPHOER) {
            return true
        }
        if (fraVedtak.beloep == null && korresponderendeUtbetalingslinje.beloep == null) {
            return true
        }
        if (fraVedtak.beloep == null || korresponderendeUtbetalingslinje.beloep == null) {
            return false
        }
        return fraVedtak.beloep.minus(korresponderendeUtbetalingslinje.beloep).abs() < BigDecimal.ONE
    }
}
