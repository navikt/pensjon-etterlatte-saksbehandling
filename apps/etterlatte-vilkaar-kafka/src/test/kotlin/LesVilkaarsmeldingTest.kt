import no.nav.etterlatte.barnepensjon.model.VilkaarService
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
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
        val meldingRevurderingDoedsfall = readFile("/melding_revurdering_doedsfall.json")
        val meldingRevurderingManueltOpphoer = readFile("/melding_revurdering_manuelt_opphoer.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesVilkaarsmelding(this, VilkaarService()) }

    @Test
    fun `skal lese og lage melding`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør
        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(3, inspector.message(0).get("vilkaarsvurderingGammel").size())
        Assertions.assertEquals(2, inspector.message(0).get("kommerSoekerTilGode").size())
        Assertions.assertEquals(8, inspector.message(0).get("kommerSoekerTilGodeGrunnlagRef").intValue())
        Assertions.assertEquals(4, inspector.message(0).get(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey).size())
    }

    @Test
    fun `skal lese og lage en melding for revurdering doedsfall`() {
        val inspector = inspector.apply { sendTestMessage(meldingRevurderingDoedsfall) }.inspektør
        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(3, inspector.message(0).get("vilkaarsvurderingGammel").size())
        Assertions.assertEquals(8, inspector.message(0).get("kommerSoekerTilGodeGrunnlagRef").intValue())
        Assertions.assertEquals(4, inspector.message(0).get(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey).size())
    }

    @Test
    fun `skal lese og lage en melding for revurdering manuelt opphoer`() {
        val inspector = inspector.apply { sendTestMessage(meldingRevurderingManueltOpphoer) }.inspektør
        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get(eventNameKey).asText())
        Assertions.assertEquals(3, inspector.message(0).get("vilkaarsvurderingGammel").size())
        Assertions.assertEquals(8, inspector.message(0).get("kommerSoekerTilGodeGrunnlagRef").intValue())
        Assertions.assertEquals(4, inspector.message(0).get(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey).size())
    }
}