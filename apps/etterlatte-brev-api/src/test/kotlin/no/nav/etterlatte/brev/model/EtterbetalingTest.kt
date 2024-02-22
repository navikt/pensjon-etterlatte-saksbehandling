package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterbetalingTest {
    @Test
    fun `skal splitte foerste og siste periode slik at perioder reflekterer oppsatt etterbetaling`() {
        val etterbetalingDto =
            EtterbetalingDTO(
                datoFom = LocalDate.of(2022, 12, 1),
                datoTom = LocalDate.of(2023, 3, 31),
            )

        val perioder =
            listOf(
                beregningsperiode(
                    datoFOM = LocalDate.of(2022, 1, 1),
                    datoTOM = LocalDate.of(2022, 12, 31),
                ),
                beregningsperiode(
                    datoFOM = LocalDate.of(2023, 1, 1),
                    datoTOM = LocalDate.of(2023, 1, 31),
                ),
                beregningsperiode(
                    datoFOM = LocalDate.of(2023, 2, 1),
                    datoTOM = null,
                ),
            )

        val etterbetalingBrev = Etterbetaling.fraBarnepensjonBeregningsperioder(etterbetalingDto, perioder)

        etterbetalingBrev shouldNotBe null
        etterbetalingBrev.fraDato shouldBe etterbetalingDto.datoFom
        etterbetalingBrev.tilDato shouldBe etterbetalingDto.datoTom
        with(etterbetalingBrev.etterbetalingsperioder) {
            size shouldBe 3
            get(0).datoFOM shouldBe LocalDate.of(2023, 2, 1)
            get(0).datoTOM shouldBe LocalDate.of(2023, 3, 31)
            get(1).datoFOM shouldBe LocalDate.of(2023, 1, 1)
            get(1).datoTOM shouldBe LocalDate.of(2023, 1, 31)
            get(2).datoFOM shouldBe LocalDate.of(2022, 12, 1)
            get(2).datoTOM shouldBe LocalDate.of(2022, 12, 31)
        }
    }

    @Test
    fun `skal kun returnere en periode som er innenfor oppsatt etterbetalingsperiode`() {
        val etterbetalingDto =
            EtterbetalingDTO(
                datoFom = LocalDate.of(2023, 1, 1),
                datoTom = LocalDate.of(2023, 1, 31),
            )

        val perioder =
            listOf(
                beregningsperiode(
                    datoFOM = LocalDate.of(2022, 1, 1),
                    datoTOM = LocalDate.of(2022, 12, 31),
                ),
                beregningsperiode(
                    datoFOM = LocalDate.of(2023, 1, 1),
                    datoTOM = LocalDate.of(2023, 1, 31),
                ),
                beregningsperiode(
                    datoFOM = LocalDate.of(2023, 2, 1),
                    datoTOM = null,
                ),
            )

        val etterbetalingBrev = Etterbetaling.fraBarnepensjonBeregningsperioder(etterbetalingDto, perioder)

        etterbetalingBrev shouldNotBe null
        etterbetalingBrev.fraDato shouldBe etterbetalingDto.datoFom
        etterbetalingBrev.tilDato shouldBe etterbetalingDto.datoTom
        with(etterbetalingBrev.etterbetalingsperioder) {
            size shouldBe 1
            get(0).datoFOM shouldBe LocalDate.of(2023, 1, 1)
            get(0).datoTOM shouldBe LocalDate.of(2023, 1, 31)
        }
    }

    @Test
    fun `skal ikke gi etterbetalingsperioder hvis beregning ikke inneholder perioder`() {
        val etterbetalingDto =
            EtterbetalingDTO(
                datoFom = LocalDate.of(2023, 1, 1),
                datoTom = LocalDate.of(2023, 1, 31),
            )

        val perioder: List<BarnepensjonBeregningsperiode> = emptyList()

        val etterbetalingBrev = Etterbetaling.fraBarnepensjonBeregningsperioder(etterbetalingDto, perioder)

        etterbetalingBrev shouldNotBe null
        etterbetalingBrev.fraDato shouldBe etterbetalingDto.datoFom
        etterbetalingBrev.tilDato shouldBe etterbetalingDto.datoTom
        etterbetalingBrev.etterbetalingsperioder shouldBe emptyList()
    }

    @Test
    fun `skal returnere en lukket periode hvis beregning kun inneholder en loepende periode`() {
        val etterbetalingDto =
            EtterbetalingDTO(
                datoFom = LocalDate.of(2024, 1, 1),
                datoTom = LocalDate.of(2024, 1, 31),
            )

        val perioder =
            listOf(
                beregningsperiode(
                    datoFOM = LocalDate.of(2024, 1, 1),
                    datoTOM = null,
                ),
            )

        val etterbetalingBrev = Etterbetaling.fraBarnepensjonBeregningsperioder(etterbetalingDto, perioder)

        etterbetalingBrev shouldNotBe null
        etterbetalingBrev.fraDato shouldBe etterbetalingDto.datoFom
        etterbetalingBrev.tilDato shouldBe etterbetalingDto.datoTom
        with(etterbetalingBrev.etterbetalingsperioder) {
            size shouldBe 1
            get(0).datoFOM shouldBe LocalDate.of(2024, 1, 1)
            get(0).datoTOM shouldBe LocalDate.of(2024, 1, 31)
        }
    }

    private fun beregningsperiode(
        datoFOM: LocalDate,
        datoTOM: LocalDate?,
    ) = BarnepensjonBeregningsperiode(
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        grunnbeloep = Kroner(120000),
        antallBarn = 1,
        utbetaltBeloep = Kroner(5000),
    )
}
