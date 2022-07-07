package model

import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.YearMonth

internal class BeregningServiceTest {
    companion object {
        val melding = readFile("/Ny.json")

        fun readmelding(file: String): Grunnlag {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@grunnlag")
            )
            return objectMapper.readValue(skjemaInfo, Grunnlag::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
    @Test
    fun beregnResultat() {
        val beregningsperioder = BeregningService().beregnResultat(readmelding("/Ny.json"), YearMonth.of(2021, 2)).beregningsperioder
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