import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.OmberegningsHendelser
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.Omberegningsnoekler
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class OmberegningsHendelserTest {

    private val behandlingService = mockk<BehandlingsService>()
    private val inspector = TestRapid().apply { OmberegningsHendelser(this, behandlingService) }

    @Test
    fun `skal opprette omberegning`() {
        val omberegningshendelseSlot = slot<Omberegningshendelse>()
        val uuid = UUID.randomUUID()

        val returnValue = mockk<HttpResponse>().also {
            every {
                runBlocking { it.body<UUID>() }
            } returns uuid
        }
        every { behandlingService.opprettOmberegning(capture(omberegningshendelseSlot)) }.returns(returnValue)

        val inspector = inspector.apply { sendTestMessage(fullMelding) }

        inspector.sendTestMessage(fullMelding)

        Assertions.assertEquals(1, omberegningshendelseSlot.captured.sakId)
        Assertions.assertEquals(2, inspector.inspektør.size)
        Assertions.assertEquals(
            uuid.toString(),
            inspector.inspektør.message(1).get(Omberegningsnoekler.omberegningId).asText()
        )
    }

    companion object {
        val fullMelding = readFile("/omberegningshendelse.json")
    }
}