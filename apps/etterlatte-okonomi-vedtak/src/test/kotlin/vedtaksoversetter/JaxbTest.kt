package vedtaksoversetter

import dummyVedtak
import no.nav.etterlatte.vedtaksoversetter.Jaxb
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import readFile

internal class JaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(dummyVedtak())
        val oppdragAsXml = Jaxb.toXml(oppdrag)

        assertNotNull(oppdragAsXml)
    }

    @Test
    fun `should convert xml to oppdrag`() {
        val oppdragXml = readFile("oppdrag_ugyldig.xml")
        val oppdrag = Jaxb.toOppdrag(oppdragXml)

        assertNotNull(oppdrag)
        assertEquals("08", oppdrag.mmel.alvorlighetsgrad)
    }
}