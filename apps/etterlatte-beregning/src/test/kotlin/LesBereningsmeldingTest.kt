
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBereningsmeldingTest {
    companion object {
        val melding = readFile("/melding.json")

        fun readmelding(file: String): Barnepensjon {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@skjema_info")
            )
            return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesBeregningsmelding(this, BeregningService()) }

    @Test
    fun `skal vurdere viklaar til gyldig`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        //TODO oppdatere testen
        //Assertions.assertEquals(3, inspector.message(0).get("@vilkaarsvurdering").size())

        //verify { fordelerMetricLogger.logMetricFordelt() }
    }
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