package no.nav.etterlatte

import no.nav.etterlatte.rivers.LagreAvkorting
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesAvkortingsmeldingTest {

    companion object {
        val melding = readFile("/melding.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
    private val inspector = TestRapid().apply { LagreAvkorting(this, vedtaksvurderingServiceMock) }

    @Test
    fun `skal lese melding`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør
        every { vedtaksvurderingServiceMock.lagreAvkorting("6", UUID.fromString("42bd0adf-6d46-43cd-84fa-ff2c06709cf7"), "noe") } returns Unit
        print(inspector.message(0))
        // Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
    }

}