package vedtaksoversetter

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.OppdragSender
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import readFile

internal class VedtaksoversetterTest {

    private val oppdragMapper = spyk<OppdragMapper>()
    private val oppdragSender = mockk<OppdragSender>(relaxed = true)

    private val inspector = TestRapid().apply {
        Vedtaksoversetter(
            rapidsConnection = this,
            oppdragMapper = oppdragMapper,
            oppdragSender = oppdragSender,
        ) }

    @Test
    @Disabled
    fun `sjekk mottak av vedtak`() {
        val inspector = inspector.apply { sendTestMessage(FATTET_VEDTAK) }.inspektør

        assertEquals("true", inspector.message(0).get("@vedtak_oversatt").asText())
        verifyOrder {
            oppdragMapper.oppdragFraVedtak(any())
            oppdragSender.sendOppdrag(any())
        }
    }

    companion object {
        val FATTET_VEDTAK = readFile("vedtak.json")
    }
}