package no.nav.etterlatte.vedtaksvurdering.klienter

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType.UTBETALING
import no.nav.etterlatte.vedtaksvurdering.vedtak
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.Month.MAY
import java.time.Month.NOVEMBER
import java.time.YearMonth.of

class SamordningsKlientTest {
    @Test
    fun `skal sette siste dato fra utbetalingsperioder som vedtakets tom-dato`() {
        val vedtak =
            vedtak(
                virkningstidspunkt = of(2024, FEBRUARY),
                utbetalingsperioder =
                    listOf(
                        Utbetalingsperiode(
                            id = 1,
                            periode = Periode(of(2024, FEBRUARY), of(2024, APRIL)),
                            beloep = BigDecimal.valueOf(70),
                            type = UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                        Utbetalingsperiode(
                            id = 2,
                            periode = Periode(of(2024, MAY), of(2024, NOVEMBER)),
                            beloep = BigDecimal.valueOf(100),
                            type = UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                    ),
            )

        val dto = vedtak.tilSamordneRequest(false)

        dto.virkFom shouldBe of(2024, FEBRUARY).atDay(1)
        dto.virkTom shouldBe of(2024, NOVEMBER).atEndOfMonth()
    }

    @Test
    fun `skal sette aapen tom-dato fra utbetalingsperioder naar seneste periode ikke har tom-dato`() {
        val vedtak =
            vedtak(
                virkningstidspunkt = of(2024, FEBRUARY),
                utbetalingsperioder =
                    listOf(
                        Utbetalingsperiode(
                            id = 1,
                            periode = Periode(of(2024, FEBRUARY), of(2024, APRIL)),
                            beloep = BigDecimal.valueOf(70),
                            type = UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                        Utbetalingsperiode(
                            id = 2,
                            periode = Periode(of(2024, MAY), null),
                            beloep = BigDecimal.valueOf(100),
                            type = UTBETALING,
                            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                        ),
                    ),
            )

        val dto = vedtak.tilSamordneRequest(true)

        dto.virkFom shouldBe of(2024, FEBRUARY).atDay(1)
        dto.virkTom shouldBe null
        dto.etterbetaling shouldBe true
    }
}
