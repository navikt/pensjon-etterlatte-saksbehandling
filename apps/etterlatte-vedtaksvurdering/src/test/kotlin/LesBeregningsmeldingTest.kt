package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.rivers.LagreBeregningsresultat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBeregningsmeldingTest {

    companion object {
        val melding = readFile("/beregningsmelding.json")
        private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { LagreBeregningsresultat(this, vedtaksvurderingServiceMock) }
    //private val beregningsresultatMock = mockk<BeregningsResultat>()

    @Test
    fun `skal lese melding`() {
        val beregningsres = slot<BeregningsResultat>()
        every { vedtaksvurderingServiceMock.lagreBeregningsresultat(any(), any(), any(), capture(beregningsres)) } returns Unit
        inspector.apply { sendTestMessage(melding) }.inspektør
        Assertions.assertEquals(BeregningsResultatType.BEREGNET, beregningsres.captured.resultat)
    }

}