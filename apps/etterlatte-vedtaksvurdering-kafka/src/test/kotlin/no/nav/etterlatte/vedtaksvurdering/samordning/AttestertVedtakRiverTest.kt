package no.nav.etterlatte.vedtaksvurdering.samordning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.VedtakService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AttestertVedtakRiverTest {
    companion object {
        val melding =
            Companion::class.java.getResource("/attestertVedtakOms.json")?.readText()
                ?: throw FileNotFoundException("Fant ikke filen")
    }

    private val vedtakService = mockk<VedtakService>()
    private val inspector = TestRapid().apply { AttestertVedtakRiver(this, vedtakService) }

    @Test
    fun `skal lese vedtak og oppdatere til samordning`() {
        val behandlingIdVal = UUID.fromString("e89c6e25-4f22-48b3-b975-4c868d830913")
        val sakIdVal = 15L
        val vedtakIdVal = 31L
        every { vedtakService.tilSamordningVedtak(behandlingIdVal) } returns
            mockk {
                every { sak } returns mockk { every { id } returns sakIdVal }
                every { behandling } returns mockk { every { id } returns behandlingIdVal }
                every { vedtakId } returns vedtakIdVal
            }

        inspector.sendTestMessage(melding)

        verify { vedtakService.tilSamordningVedtak(behandlingIdVal) }
    }
}
