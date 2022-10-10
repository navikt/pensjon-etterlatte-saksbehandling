package beregning

import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.beregning.erInklusiv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class BeregningsResultatKtTest {
    val periode = SoeskenPeriode(
        datoFOM = YearMonth.of(2022, 1),
        datoTOM = YearMonth.of(2022, 11),
        soeskenFlokk = emptyList()
    )

    @Test
    fun `SoeskenPeriode-erInklusiv skal returnere true naar to datoer er innenfor SoeskenPerioden`() {
        assertEquals(true, periode.erInklusiv(YearMonth.of(2022, 1), YearMonth.of(2022, 11)))
        assertEquals(true, periode.erInklusiv(YearMonth.of(2022, 2), YearMonth.of(2022, 11)))
        assertEquals(true, periode.erInklusiv(YearMonth.of(2022, 1), YearMonth.of(2022, 10)))
        assertEquals(true, periode.erInklusiv(YearMonth.of(2022, 2), YearMonth.of(2022, 10)))
        assertEquals(true, periode.erInklusiv(YearMonth.of(2022, 5), YearMonth.of(2022, 8)))
    }

    @Test
    fun `SoeskenPeriode-erInklusiv skal returnere false om SoeskenPeriode ikke er mellom begge datoene`() {
        assertEquals(false, periode.erInklusiv(YearMonth.of(2022, 8), YearMonth.of(2022, 12)))
        assertEquals(false, periode.erInklusiv(YearMonth.of(2021, 1), YearMonth.of(2022, 5)))
    }

    @Test
    fun `SoeskenPeriode-erInklusiv skal returnere false om datoene wrapper rundt SoeskenPeriode`() {
        assertEquals(false, periode.erInklusiv(YearMonth.of(2021, 12), YearMonth.of(2022, 12)))
    }
}