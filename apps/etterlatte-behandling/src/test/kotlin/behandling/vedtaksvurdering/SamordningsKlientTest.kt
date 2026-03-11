package no.nav.etterlatte.behandling.vedtaksvurdering

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.vedtaksvurdering.klienter.tilSamordneRequest
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth

class SamordningsKlientTest {
    @Test
    fun `skal sette siste dato fra utbetalingsperioder som vedtakets tom-dato`() {
        val vedtak =
            vedtak(
                virkningstidspunkt = YearMonth.of(2024, Month.FEBRUARY),
                utbetalingsperioder =
                    listOf(
                        Utbetalingsperiode(
                            id = 1,
                            periode = Periode(YearMonth.of(2024, Month.FEBRUARY), YearMonth.of(2024, Month.APRIL)),
                            beloep = BigDecimal.valueOf(70),
                            type = UtbetalingsperiodeType.UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                        Utbetalingsperiode(
                            id = 2,
                            periode = Periode(YearMonth.of(2024, Month.MAY), YearMonth.of(2024, Month.NOVEMBER)),
                            beloep = BigDecimal.valueOf(100),
                            type = UtbetalingsperiodeType.UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                    ),
            )

        val dto = vedtak.tilSamordneRequest(EtterbetalingResultat.ingen())

        dto.virkFom shouldBe YearMonth.of(2024, Month.FEBRUARY).atDay(1)
        dto.virkTom shouldBe YearMonth.of(2024, Month.NOVEMBER).atEndOfMonth()
    }

    @Test
    fun `skal sette aapen tom-dato fra utbetalingsperioder naar seneste periode ikke har tom-dato`() {
        val vedtak =
            vedtak(
                virkningstidspunkt = YearMonth.of(2024, Month.FEBRUARY),
                utbetalingsperioder =
                    listOf(
                        Utbetalingsperiode(
                            id = 1,
                            periode = Periode(YearMonth.of(2024, Month.FEBRUARY), YearMonth.of(2024, Month.APRIL)),
                            beloep = BigDecimal.valueOf(70),
                            type = UtbetalingsperiodeType.UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                        Utbetalingsperiode(
                            id = 2,
                            periode = Periode(YearMonth.of(2024, Month.MAY), null),
                            beloep = BigDecimal.valueOf(100),
                            type = UtbetalingsperiodeType.UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                    ),
            )

        val dto = vedtak.tilSamordneRequest(EtterbetalingResultat(erEtterbetaling = true))

        dto.virkFom shouldBe YearMonth.of(2024, Month.FEBRUARY).atDay(1)
        dto.virkTom shouldBe null
        dto.etterbetaling shouldBe true
    }
}
