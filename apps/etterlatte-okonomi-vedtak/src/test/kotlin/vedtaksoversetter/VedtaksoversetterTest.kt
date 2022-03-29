package no.nav.etterlatte.vedtaksoversetter

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verifyOrder
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class VedtaksoversetterTest {

    private val oppdragMapper = spyk<OppdragMapper>()
    private val oppdragSender = mockk<OppdragSender>(relaxed = true)
    private val oppdragRepository = mockk<UtbetalingsoppdragDao>(relaxed = true)

    private val inspector = TestRapid().apply {
        Vedtaksoversetter(
            rapidsConnection = this,
            oppdragMapper = oppdragMapper,
            oppdragSender = oppdragSender,
            utbetalingsoppdragDao = oppdragRepository
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