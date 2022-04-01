package no.nav.etterlatte.oppdrag

import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class VedtakMottakerTest {

    private val oppdragService = mockk<OppdragService>(relaxed = true)

    private val inspector = TestRapid().apply {
        VedtakMottaker(
            rapidsConnection = this,
            oppdragService = oppdragService,
        ) }

    @Test
    fun `sjekk mottak av vedtak med attestasjon`() {
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        verifyOrder {
            oppdragService.opprettOgSendOppdrag(any(), any())
        }
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak.json")
    }
}