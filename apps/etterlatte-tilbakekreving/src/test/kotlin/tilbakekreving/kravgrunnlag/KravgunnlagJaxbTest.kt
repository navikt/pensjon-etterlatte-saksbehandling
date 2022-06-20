package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.testsupport.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class KravgunnlagJaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val kravgrunnlagXml = readFile("/kravgrunnlag.xml")
        val kravgrunnlag = KravgrunnlagJaxb.toKravgrunnlag(kravgrunnlagXml)

        assertNotNull(kravgrunnlag)
        assertEquals(BigInteger.valueOf(302004), kravgrunnlag.kravgrunnlagId)
    }
}