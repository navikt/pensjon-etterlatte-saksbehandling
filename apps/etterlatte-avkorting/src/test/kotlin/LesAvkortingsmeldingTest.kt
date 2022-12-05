import model.AvkortingService
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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

    @Test
    fun `skal vurdere viklaar til gyldig`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(BehandlingGrunnlagEndret.eventName, inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(4, inspector.message(0).get("vilkaarsvurdering").size())
        Assertions.assertEquals(5, inspector.message(0).get("beregning").size())
    }
}