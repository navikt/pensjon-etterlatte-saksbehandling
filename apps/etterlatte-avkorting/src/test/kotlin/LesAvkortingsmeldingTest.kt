
import no.nav.etterlatte.model.AvkortingService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesAvkortingsmeldingTest {
    companion object {
        val melding = readFile("/meldingNy.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesAvkortingsmelding(this, AvkortingService()) }

    //TODO fikse denne
    //@Test
    fun `skal vurdere viklaar til gyldig`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        //TODO oppdatere testen
        //Assertions.assertEquals(3, inspector.message(0).get("vilkaarsvurdering").size())

        //verify { fordelerMetricLogger.logMetricFordelt() }
    }
}