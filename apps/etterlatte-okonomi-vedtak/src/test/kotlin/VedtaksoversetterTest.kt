import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksoversetterTest {

    private val inspector = TestRapid().apply { Vedtaksoversetter(this, oppdragsMapper = OppdragMapper) }

    @Test
    fun `sjekk mottak av vedtak`() {
        val inspector = inspector.apply { sendTestMessage(TOMT_VEDTAK) }.inspektør
        assertEquals("true", inspector.message(0).get("@vedtak_oversatt").asText())
    }

    companion object {
        val TOMT_VEDTAK = readFile("tomt_vedtak.json")
    }
}