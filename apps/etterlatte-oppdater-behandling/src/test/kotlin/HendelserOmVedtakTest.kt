import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.HendelserOmVedtak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class HendelserOmVedtakTest {
    private val behandlingService = mockk<BehandlingsService>()

    private val inspector = TestRapid().apply { HendelserOmVedtak(this, behandlingService) }
    private val behandlingId = UUID.randomUUID()
    private val hendelse = "FATTET"

    val fullMelding = """{
  "@event_name": "VEDTAK:$hendelse",
  "behandlingId": "$behandlingId",
  "vedtakId": 1,
  "sakId": 2,
  "eventtimestamp": "${Tidspunkt.now().instant}"
}"""

    @Test
    fun skalLeseVedtakHendelser() {

        val behandlingIdSlot = slot<UUID>()
        val hendelseSlot = slot<String>()
        every {
            behandlingService.vedtakHendelse(capture(behandlingIdSlot),
                capture(hendelseSlot),
                any(),
                any(),
                any(),
                any(),
                any())
        }.returns(Unit)

        inspector.sendTestMessage(fullMelding)

        assertEquals(behandlingId, behandlingIdSlot.captured)
        assertEquals(hendelse, hendelseSlot.captured)
    }
}
