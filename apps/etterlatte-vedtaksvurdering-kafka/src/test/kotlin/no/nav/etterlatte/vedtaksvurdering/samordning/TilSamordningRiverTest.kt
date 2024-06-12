package no.nav.etterlatte.vedtaksvurdering.samordning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.SamordneResponse
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.samordning.TilSamordningRiver
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilSamordningRiverTest {
    companion object {
        val melding =
            Companion::class.java
                .getResource("/attestertVedtakOms.json")
                ?.readText()
                ?.replace("ATTESTERT", "TIL_SAMORDNING")
                ?: throw FileNotFoundException("Fant ikke filen")
    }

    private val vedtakService = mockk<VedtakService>()
    private val inspector = TestRapid().apply { TilSamordningRiver(this, vedtakService) }

    @Test
    fun `skal kalle samordne, og deretter oppdatere status til samordnet pga ikke vente`() {
        val behandlingId = UUID.fromString("e89c6e25-4f22-48b3-b975-4c868d830913")
        val vedtakId = 31L
        every { vedtakService.samordneVedtak(behandlingId) } returns SamordneResponse(skalVentePaaSamordning = false)
        every { vedtakService.samordnetVedtak(vedtakId.toString()) } returns mockk()

        inspector.sendTestMessage(melding)

        verify { vedtakService.samordneVedtak(behandlingId) }
        verify { vedtakService.samordnetVedtak(vedtakId.toString()) }
    }

    @Test
    fun `skal kalle samordne, og deretter ikke oppdatere status til samordnet fordi det skal ventes paa ekstern samordning`() {
        val behandlingId = UUID.fromString("e89c6e25-4f22-48b3-b975-4c868d830913")
        val vedtakId = 31L
        every { vedtakService.samordneVedtak(behandlingId) } returns SamordneResponse(skalVentePaaSamordning = true)

        inspector.sendTestMessage(melding)

        verify { vedtakService.samordneVedtak(behandlingId) }
        verify(exactly = 0) { vedtakService.samordnetVedtak(vedtakId.toString()) }
    }
}
