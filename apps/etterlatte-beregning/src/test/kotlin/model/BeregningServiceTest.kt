package model

import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

internal class BeregningServiceTest {

    @Test
    fun beregnResultat() {
        val beregningsperioder = BeregningService().beregnResultat(emptyList(), LocalDate.of(2021, 2, 1)).beregningsperioder
        beregningsperioder[0].also {
            assertEquals(LocalDate.of(2021,2,1), it.datoFOM.toLocalDate())
            assertEquals(LocalDate.of(2021,4,30), it.datoTOM?.toLocalDate())
        }
        beregningsperioder[1].also {
            assertEquals(LocalDate.of(2021,5,1), it.datoFOM.toLocalDate())
            assertEquals(LocalDate.of(2022,4,30), it.datoTOM?.toLocalDate())
        }
        beregningsperioder[2].also {
            assertEquals(LocalDate.of(2022,5,1), it.datoFOM.toLocalDate())
            assertNull(it.datoTOM)
        }
    }
}