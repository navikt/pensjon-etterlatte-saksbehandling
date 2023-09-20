package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.testsupport.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class KravgunnlagJaxbTest {
    @Test
    fun `skal lese og mappe kravgrunnlag fra tilbakekrevingskomponenten`() {
        val kravgrunnlagXml = readFile("/kravgrunnlag.xml")
        val kravgrunnlag = KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(kravgrunnlagXml)

        assertNotNull(kravgrunnlag)
        assertEquals(BigInteger.valueOf(302004), kravgrunnlag.kravgrunnlagId)
    }
}
