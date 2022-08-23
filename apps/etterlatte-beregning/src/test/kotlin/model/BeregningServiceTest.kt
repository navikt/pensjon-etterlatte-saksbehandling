package model

import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.beregnSisteTom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate
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
            assertEquals(YearMonth.of(2021, 2), it.datoFOM)
            assertEquals(YearMonth.of(2021, 4), it.datoTOM)
        }
        beregningsperioder[1].also {
            assertEquals(YearMonth.of(2021, 5), it.datoFOM)
            assertEquals(YearMonth.of(2021, 8), it.datoTOM)
        }
        beregningsperioder[2].also {
            assertEquals(YearMonth.of(2021, 9), it.datoFOM)
            assertEquals(YearMonth.of(2021, 11), it.datoTOM)
        }
        beregningsperioder[3].also {
            assertEquals(YearMonth.of(2021, 12), it.datoFOM)
            assertEquals(null, it.datoTOM)
        }
    }

    @Test
    fun `beregningsperiodene får riktig beløp`() {
        assertEquals(2745, beregningsperioder[0].utbetaltBeloep)
        assertEquals(2882, beregningsperioder[1].utbetaltBeloep)
        assertEquals(2882, beregningsperioder[2].utbetaltBeloep)
        assertEquals(3547, beregningsperioder[3].utbetaltBeloep)
    }

    @Nested
    class beregnSisteTom {
        @Test
        fun `skal returnere fødselsdato om søker blir 18 i løpet av perioden`() {
            val fødselsdato = LocalDate.of(2004, 3, 23)
            assertEquals(YearMonth.of(2022, 3), beregnSisteTom(fødselsdato, YearMonth.of(2022, 3)))

            val fødselsdato2 = LocalDate.of(2004, 2, 23)
            assertEquals(YearMonth.of(2022, 2), beregnSisteTom(fødselsdato2, YearMonth.of(2022, 3)))
        }

        @Test
        fun `skal returnere null om søker er under 18 i hele perioden`() {
            val fødselsdato = LocalDate.of(2004, 4, 23)
            assertEquals(null, beregnSisteTom(fødselsdato, YearMonth.of(2022, 3)))
        }
    }
}