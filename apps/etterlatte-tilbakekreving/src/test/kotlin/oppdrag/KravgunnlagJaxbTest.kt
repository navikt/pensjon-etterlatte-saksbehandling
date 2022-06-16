package oppdrag

import no.nav.etterlatte.tilbakekreving.oppdrag.KravgrunnlagJaxb
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import readFile

internal class KravgunnlagJaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val kravgrunnlag = readFile("/tilbakekrevingsvedtak.xml")
        val kravgrunnlagObjekt = KravgrunnlagJaxb.toKravgrunnlag(kravgrunnlag)

        assertNotNull(kravgrunnlagObjekt)
    }
}