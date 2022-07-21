import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.OppdaterBehandling
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

internal class OppdaterBehandlingTest {

    private val behandlingService = mockk<BehandlingsService>()

    private val inspector = TestRapid().apply { OppdaterBehandling(this, behandlingService) }

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        val sakIdSlot = slot<Long>()
        every { behandlingService.grunnlagEndretISak(capture(sakIdSlot)) }.returns(Unit)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(3L, sakIdSlot.captured)
    }


    companion object {
        val fullMelding = readFile("/nyere.json")

    }
}
fun readFile(file: String) = OppdaterBehandlingTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")