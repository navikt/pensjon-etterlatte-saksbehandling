import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.OppdaterBehandling
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

internal class OppdaterBehandlingTest {

    private val behandlingService = mockk<BehandlingsService>()

    private val inspector = TestRapid().apply { OppdaterBehandling(this, behandlingService) }

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        //every { behandlingService.leggTilVilkaarsresultat(any(),any()) }.returns(Unit)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }.inspektør

        //Assertions.assertEquals("ey_fordelt", inspector.message(0).get("@event").asText())
        //Assertions.assertEquals("true", inspector.message(0).get("@soeknad_fordelt").asText())

    }


    companion object {
        val fullMelding = readFile("/fullMelding.json")

    }
}
fun readFile(file: String) = OppdaterBehandlingTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")