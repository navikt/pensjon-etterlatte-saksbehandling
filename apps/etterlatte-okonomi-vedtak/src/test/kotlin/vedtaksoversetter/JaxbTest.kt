package vedtaksoversetter

import dummyVedtak
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.toXml
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class JaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(dummyVedtak())
        val oppdragAsXml = oppdrag.toXml()

        assertNotNull(oppdragAsXml)
    }
}