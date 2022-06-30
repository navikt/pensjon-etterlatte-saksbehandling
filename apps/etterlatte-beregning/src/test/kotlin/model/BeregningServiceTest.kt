package model

import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class BeregningServiceTest {

    @Test
    fun beregnResultat() {
        val beregningsperioder = BeregningService().beregnResultat(null, YearMonth.of(2021, 2)).beregningsperioder
        beregningsperioder[0].also {
            assertEquals(YearMonth.of(2021,2,), it.datoFOM)
            assertEquals(YearMonth.of(2021,4), it.datoTOM)
        }
        beregningsperioder[1].also {
            assertEquals(YearMonth.of(2021,5), it.datoFOM)
            assertEquals(YearMonth.of(2022,4), it.datoTOM)
        }
        beregningsperioder[2].also {
            assertEquals(YearMonth.of(2022,5), it.datoFOM)
            assertNull(it.datoTOM)
        }
    }
}