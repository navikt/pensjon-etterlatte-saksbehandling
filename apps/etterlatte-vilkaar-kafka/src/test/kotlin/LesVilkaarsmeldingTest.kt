import no.nav.etterlatte.barnepensjon.model.VilkaarService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
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
    fun `skal vurdere vilkaar til gyldig`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(3, inspector.message(0).get("vilkaarsvurdering").size())
        Assertions.assertEquals(2, inspector.message(0).get("kommerSoekerTilGode").size())
        Assertions.assertEquals(8, inspector.message(0).get("vilkaarsvurderingGrunnlagRef").intValue())

    }
}
