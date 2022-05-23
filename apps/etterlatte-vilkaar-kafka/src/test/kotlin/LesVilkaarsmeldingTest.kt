
import no.nav.etterlatte.model.VilkaarService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesVilkaarsmeldingTest {
    companion object {
        val melding = readFile("/melding.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesVilkaarsmelding(this, VilkaarService()) }

    @Test
    fun `skal vurdere viklaar til gyldig`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        Assertions.assertEquals(3, inspector.message(0).get("@vilkaarsvurdering").size())
        Assertions.assertEquals(3, inspector.message(0).get("@kommersoekertilgode").size())

    }
}