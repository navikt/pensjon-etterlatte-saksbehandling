package no.nav.etterlatte.utbetaling.avstemming.vedtak

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingsperiodeType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth

class Vedtaksverifiserer(
    private val repository: UtbetalingDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiser(vedtak: Utbetalingsvedtak) {
        val utbetaling = repository.hentUtbetaling(vedtakId = vedtak.vedtakId)
        if (utbetaling == null) {
            logger.warn("Ingen utbetaling for vedtak ${vedtak.vedtakId}. Returnerer")
            return
        }
        sammenlignLinjer(utbetaling, vedtak)
    }

    private fun sammenlignLinjer(
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
