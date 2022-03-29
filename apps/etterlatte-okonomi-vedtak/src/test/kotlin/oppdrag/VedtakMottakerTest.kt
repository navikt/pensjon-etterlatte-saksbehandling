package no.nav.etterlatte.oppdrag

import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class VedtakMottakerTest {

    private val oppdragService = mockk<OppdragService>(relaxed = true)

    private val inspector = TestRapid().apply {
        VedtakMottaker(
            rapidsConnection = this,
            oppdragService = oppdragService,
        ) }

    @Test
    @Disabled
    fun `sjekk mottak av vedtak`() {
        val inspector = inspector.apply { sendTestMessage(FATTET_VEDTAK) }.inspektør

        assertEquals("true", inspector.message(0).get("@vedtak_oversatt").asText())
        verifyOrder {
            oppdragService.opprettOgSendOppdrag(any())
        }
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak.json")
    }
}