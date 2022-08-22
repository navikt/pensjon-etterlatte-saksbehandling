package model

import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.YearMonth

internal class BeregningServiceTest {
    companion object {
        val melding = readFile("/Nyere.json")

        fun readmelding(file: String): Grunnlag {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("grunnlag")
            )
            return objectMapper.readValue(skjemaInfo, Grunnlag::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val beregningsperioder = BeregningService().beregnResultat(
        readmelding("/Nyere.json"),
        YearMonth.of(2021, 2),
        YearMonth.of(2021, 9)
    ).beregningsperioder

    @Test
    fun beregnResultat() {
        beregningsperioder[0].also {
            Assertions.assertEquals(YearMonth.of(2021, 2), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2021, 4), it.datoTOM)
        }
        beregningsperioder[1].also {
            Assertions.assertEquals(YearMonth.of(2021, 5), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2021, 8), it.datoTOM)
        }
        beregningsperioder[2].also {
            Assertions.assertEquals(YearMonth.of(2021, 9), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2021, 11), it.datoTOM)
        }
        beregningsperioder[3].also {
            Assertions.assertEquals(YearMonth.of(2021, 12), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2022, 4), it.datoTOM)
        }
    }

    @Test
    fun `beregningsperiodene får riktig beløp`() {
        Assertions.assertEquals(2745, beregningsperioder[0].utbetaltBeloep)
        Assertions.assertEquals(2882, beregningsperioder[1].utbetaltBeloep)
        Assertions.assertEquals(2882, beregningsperioder[2].utbetaltBeloep)
        Assertions.assertEquals(3547, beregningsperioder[3].utbetaltBeloep)
    }
}