
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBereningsmeldingTest {
    companion object {
        val melding = readFile("/Nyere.json")

        fun readmelding(file: String): Grunnlag {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@grunnlag")
            )
            return objectMapper.readValue(skjemaInfo, Grunnlag::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesBeregningsmelding(this, BeregningService()) }

    @Test
    fun `skal beregne en melding som er vilkaarsvurdert`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        Assertions.assertEquals("BEREGNET", inspector.message(0).get("@beregning").get("resultat").asText())
        println("bah")

    }
}