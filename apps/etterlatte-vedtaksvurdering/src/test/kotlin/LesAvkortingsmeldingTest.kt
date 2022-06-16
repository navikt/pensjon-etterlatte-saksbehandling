package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.rivers.LagreAvkorting
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesAvkortingsmeldingTest {

    companion object {
        val melding = readFile("/meldingNy.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
    private val inspector = TestRapid().apply { LagreAvkorting(this, vedtaksvurderingServiceMock) }

    @Test
    fun `skal lese melding`() {
        //TODO her må vi kanskje endre til avkortingsresultat
        val avkortningsres = slot<AvkortingsResultat>()
        every { vedtaksvurderingServiceMock.lagreAvkorting(any(), any(), any(), capture(avkortningsres)) } returns Unit
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

    }

}