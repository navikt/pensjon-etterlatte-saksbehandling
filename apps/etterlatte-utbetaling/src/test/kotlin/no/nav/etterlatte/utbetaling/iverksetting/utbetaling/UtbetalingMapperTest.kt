package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import Regelverk
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.utbetaling.utbetalingsvedtak
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth

class UtbetalingMapperTest {
    @Test
    fun `skal gi riktig klassifikasjonskode for barnepensjon foer og etter regelverksendring fra 2024`() {
        val mapper =
            UtbetalingMapper(
                emptyList(),
                utbetalingsvedtak(
                    utbetalingsperioder =
                        listOf(
                            utbetalingsperiode(
                                YearMonth.of(2023, Month.JANUARY),
                                YearMonth.of(2023, Month.DECEMBER),
                                Regelverk.REGELVERK_TOM_DES_2023,
                            ),
                            utbetalingsperiode(
                                YearMonth.of(2024, Month.JANUARY),
                                null,
                                Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                    sakType = Saktype.BARNEPENSJON,
                ),
            )

        val utbetaling = mapper.opprettUtbetaling()

        val (utbetalingsperiode2023, utbetalingsperiode2024) = utbetaling.utbetalingslinjer
        utbetalingsperiode2023.klassifikasjonskode shouldBe OppdragKlassifikasjonskode.BARNEPEFOER2024_OPTP
        utbetalingsperiode2024.klassifikasjonskode shouldBe OppdragKlassifikasjonskode.BARNEPENSJON_OPTP
    }

    @Test
    fun `skal gi riktig klassifikasjonskode for barnepensjon foer og etter regelverksendring fra 2024 uten regelverk satt`() {
        val mapper =
            UtbetalingMapper(
                emptyList(),
                utbetalingsvedtak(
                    utbetalingsperioder =
                        listOf(
                            utbetalingsperiode(
                                YearMonth.of(2023, Month.JANUARY),
                                YearMonth.of(2023, Month.DECEMBER),
                            ),
                            utbetalingsperiode(
                                YearMonth.of(2024, Month.JANUARY),
                            ),
                        ),
                    sakType = Saktype.BARNEPENSJON,
                ),
            )

        val utbetaling = mapper.opprettUtbetaling()

        val (utbetalingsperiode2023, utbetalingsperiode2024) = utbetaling.utbetalingslinjer
        utbetalingsperiode2023.klassifikasjonskode shouldBe OppdragKlassifikasjonskode.BARNEPEFOER2024_OPTP
        utbetalingsperiode2024.klassifikasjonskode shouldBe OppdragKlassifikasjonskode.BARNEPENSJON_OPTP
    }

    @Test
    fun `skal gi riktig klassifikasjonskode for omstillingsstoenad fra 2024`() {
        val mapper =
            UtbetalingMapper(
                emptyList(),
                utbetalingsvedtak(
                    utbetalingsperioder =
                        listOf(
                            utbetalingsperiode(
                                YearMonth.of(2024, Month.JANUARY),
                                null,
                                Regelverk.REGELVERK_FOM_JAN_2024,
                            ),
                        ),
                    sakType = Saktype.OMSTILLINGSSTOENAD,
                ),
            )

        val utbetaling = mapper.opprettUtbetaling()

        utbetaling.utbetalingslinjer.first().klassifikasjonskode shouldBe OppdragKlassifikasjonskode.OMSTILLINGSTOENAD_OPTP
    }

    private fun utbetalingsperiode(
        fom: YearMonth,
        tom: YearMonth? = null,
        regelverk: Regelverk? = null,
    ) = Utbetalingsperiode(
        id = 1,
        periode = Periode(fom = fom, tom),
        beloep = BigDecimal.valueOf(2000),
        type = UtbetalingsperiodeType.UTBETALING,
        regelverk = regelverk,
    )
}
