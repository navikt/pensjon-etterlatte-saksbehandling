package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.rivers.LagreKommerSoekerTilgodeResultat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesKommerSoekerTilgodeMeldingTest {

    companion object {
        val melding = readFile("/meldingNy.json")
        private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val inspector = TestRapid().apply { LagreKommerSoekerTilgodeResultat(this, vedtaksvurderingServiceMock) }

    @Test
    fun `skal lese melding`() {
        val tilgoderesultat = slot<KommerSoekerTilgode>()
        every { vedtaksvurderingServiceMock.lagreKommerSoekerTilgodeResultat(any(), any(), any(), capture(tilgoderesultat)) } returns Unit
        inspector.apply { sendTestMessage(melding) }.inspektør
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, tilgoderesultat.captured.kommerSoekerTilgodeVurdering.resultat)
    }

}