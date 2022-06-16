package oppdrag

import junit.framework.TestCase.assertNotNull
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.oppdrag.TilbakekrevingsmeldingMapper
import org.junit.jupiter.api.Test
import readFile

internal class TilbakekrevingsmeldingMapperTest {

    @Test
    fun `skal deserialisere tilbakekrevingsmelding fra xml`() {
        val kravgrunnlag = readFile("/tilbakekrevingsvedtak.txt")
        val kravgrunnlagObjekt = TilbakekrevingsmeldingMapper().toTilbakekreving(kravgrunnlag)
        println(kravgrunnlagObjekt.toJson())
        assertNotNull(kravgrunnlagObjekt)
    }
}