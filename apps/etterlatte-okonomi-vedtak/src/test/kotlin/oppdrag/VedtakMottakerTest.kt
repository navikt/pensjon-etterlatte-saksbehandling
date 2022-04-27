package no.nav.etterlatte.oppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.oppdrag
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtakMottakerTest {

    private val oppdragService = mockk<OppdragService>(relaxed = true) {
        every { opprettOgSendOppdrag(any(), any()) } returns mockk {
            every { status } returns UtbetalingsoppdragStatus.SENDT
            every { utgaendeOppdrag } returns oppdrag("3")
        }
    }


    private val inspector = TestRapid().apply {
        VedtakMottaker(
            rapidsConnection = this,
            oppdragService = oppdragService,
        )
    }

    @Test
    fun `attestert vedtak mottatt korrekt og utbetaling_oppdatert-event postet`() {
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_oppdatert", get("@event_name").textValue())
            assertEquals("3", get("@vedtakId").textValue())
            assertEquals("SENDT", get("@status").textValue())
        }
    }

    @Test
    fun `mottatt vedtak eksisterer fra for og poster utbetaling eksisterer-melding`() {
        every { oppdragService.oppdragEksistererFraFor(any()) } returns true
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_eksisterer", get("@event_name").textValue())
            assertEquals("3", get("@vedtakId").textValue())
        }
    }


    @Test
    fun `mottatt vedtak forer til feil og publiserer utbetaling feilet-melding`() {
        every { oppdragService.oppdragEksistererFraFor(any()) } throws Exception()
        assertThrows<Exception> { inspector.apply { sendTestMessage(FATTET_VEDTAK) } }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_feilet", get("@event_name").textValue())
            assertEquals("3", get("@vedtakId").textValue())
        }
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak3.json")
    }
}