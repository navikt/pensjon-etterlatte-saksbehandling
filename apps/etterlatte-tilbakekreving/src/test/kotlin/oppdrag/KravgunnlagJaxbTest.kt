package oppdrag

import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.tilbakekreving.oppdrag.KravgrunnlagJaxb
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import readFile

internal class KravgunnlagJaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val kravgrunnlag = readFile("/tilbakekrevingsvedtak.txt")
        val kravgrunnlagObjekt = KravgrunnlagJaxb.toKravgrunnlag(kravgrunnlag)

        Assertions.assertNotNull(kravgrunnlagObjekt)

        println(kravgrunnlagObjekt.toJson())
    }
}