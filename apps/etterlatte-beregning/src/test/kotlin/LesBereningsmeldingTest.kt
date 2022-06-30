
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBereningsmeldingTest {
    companion object {
        val melding = readFile("/Ny.json")

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
        val beregningsperioder = BeregningService().beregnResultat(null, YearMonth.of(2021, 2)).beregningsperioder
        beregningsperioder[0].also {
            Assertions.assertEquals(YearMonth.of(2021,2), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2021,4), it.datoTOM)
        }
        beregningsperioder[1].also {
            Assertions.assertEquals(YearMonth.of(2021,5), it.datoFOM)
            Assertions.assertEquals(YearMonth.of(2022,4), it.datoTOM)
        }
        beregningsperioder[2].also {
            Assertions.assertEquals(YearMonth.of(2022,5), it.datoFOM)
            Assertions.assertNull(it.datoTOM)
        }
    }
}