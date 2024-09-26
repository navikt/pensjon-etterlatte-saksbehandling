package no.nav.etterlatte.vedtaksvurdering.rivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.behandling.randomSakId
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LagreIverksattVedtakRiverTest {
    companion object {
        val melding = readFile("/utbetalingsmelding.json")

        fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText() ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val vedtaksvurderingServiceMock = mockk<VedtakService>()
    private val inspector = spyk(TestRapid().apply { LagreIverksattVedtakRiver(this, vedtaksvurderingServiceMock) })

    @Test
    fun `skal lese utbetalingsmelding`() {
        val behandlingIdVal = UUID.fromString("45dc0f0e-dbd0-465c-880b-f20ddb8e3789")
        val sakIdVal = randomSakId()
        val vedtakIdVal = 1L
        val behandlingIdSlot = slot<UUID>()
        every { vedtaksvurderingServiceMock.iverksattVedtak(capture(behandlingIdSlot)) } returns
            mockk {
                every { sak } returns mockk { every { id } returns sakIdVal }
                every { behandlingId } returns behandlingIdVal
                every { id } returns vedtakIdVal
            }

        inspector.apply { sendTestMessage(melding) }.inspektør
        Assertions.assertEquals(behandlingIdVal, behandlingIdSlot.captured)
    }
}
