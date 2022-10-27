package vedtaksvurdering.rivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreVirkningstidspunkt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesVirkningstidspunktTest {

    companion object {
        val melding = readFile("/kommerSoekerTilGodeMelding.json")
        private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()

        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LagreVirkningstidspunkt(this, vedtaksvurderingServiceMock) }

    @Test
    fun `skal lese melding og konvertere virkningstidspunkt fra YearMonth til LocalDate`() {
        val virkningstidspunkt = slot<LocalDate>()
        every {
            vedtaksvurderingServiceMock.lagreVirkningstidspunkt(
                any(),
                any(),
                capture(virkningstidspunkt)
            )
        } returns Unit
        inspector.apply { sendTestMessage(melding) }.inspektør
        assertEquals(virkningstidspunkt.captured, LocalDate.of(2022, 3, 1))
    }
}