import io.mockk.every
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
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

        fun readmelding(file: String): Barnepensjon {
            val skjemaInfo = objectMapper.writeValueAsString(
                objectMapper.readTree(readFile(file)).get("@skjema_info")
            )
            return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
        }

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LesVilkaarsmelding(this, VilkaarService()) }

    @Test
    fun `skal vurdere viklaar til gyldig`() {

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
        Assertions.assertEquals(4, inspector.message(0).get("@vilkaarsvurdering").size())

        //verify { fordelerMetricLogger.logMetricFordelt() }
    }
}