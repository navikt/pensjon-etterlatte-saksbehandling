package vedtaksoversetter

import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import readFile

internal class VedtaksoversetterTest {

    private val oppdragMapper = spyk<OppdragMapper>()
    private val inspector = TestRapid().apply { Vedtaksoversetter(this, oppdragMapper = oppdragMapper) }

    @Test
    fun `sjekk mottak av vedtak`() {
        val inspector = inspector.apply { sendTestMessage(FATTET_VEDTAK) }.inspektør

        assertEquals("true", inspector.message(0).get("@vedtak_oversatt").asText())
        verify { oppdragMapper.oppdragFraVedtak(any()) }
    }

    companion object {
        val FATTET_VEDTAK = readFile("vedtak.json")
    }
}