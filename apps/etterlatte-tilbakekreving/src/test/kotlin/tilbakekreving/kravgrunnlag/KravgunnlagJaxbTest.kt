package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.tilbakekreving.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class KravgunnlagJaxbTest {
    @Test
    fun `skal lese og mappe kravgrunnlag fra tilbakekrevingskomponenten`() {
        val xml = readFile("/kravgrunnlag.xml")

        val kravgrunnlag = KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(xml)

        assertNotNull(kravgrunnlag)
        assertEquals(BigInteger.valueOf(302004), kravgrunnlag.kravgrunnlagId)
    }

    @Test
    fun `skal lese og mappe kravOgVedtakStatus fra tilbakekrevingskomponenten`() {
        val xml = readFile("/krav_og_vedtak_status.xml")

        val kravOgVedtakstatus = KravgrunnlagJaxb.toKravOgVedtakstatus(xml)

        assertNotNull(kravOgVedtakstatus)
        assertEquals("1", kravOgVedtakstatus.fagsystemId)
    }
}
