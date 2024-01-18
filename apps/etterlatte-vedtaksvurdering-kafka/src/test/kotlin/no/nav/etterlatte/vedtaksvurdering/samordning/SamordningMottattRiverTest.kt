package no.nav.etterlatte.vedtaksvurdering.samordning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SamordningMottattRiverTest {
    companion object {
        val melding =
            """
            {
              "@event_name": "VEDTAK:SAMORDNING_MOTTATT",
              "vedtakId": "67300342"
            }
            """.trimIndent()
    }

    private val vedtakService = mockk<VedtakService>()
    private val inspector = TestRapid().apply { SamordningMottattRiver(this, vedtakService) }

    @Test
    fun `skal lese vedtak og oppdatere til samordnet`() {
        val vedtakId = "67300342"
        every { vedtakService.samordnetVedtak(vedtakId) } returns
            mockk<VedtakDto> {
                every { behandlingId } returns UUID.randomUUID()
            }

        inspector.sendTestMessage(melding)

        verify { vedtakService.samordnetVedtak(vedtakId) }
    }
}
