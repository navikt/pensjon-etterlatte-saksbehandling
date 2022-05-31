package no.nav.etterlatte.utbetaling.iverksetting

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Datatype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.EksisterendeData
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakId
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtakMottakerTest {

    private val utbetalingService = mockk<UtbetalingService>(relaxed = true) {
        every { iverksettUtbetaling(any()) } returns mockk {
            every { status } returns UtbetalingStatus.SENDT
            every { oppdrag } returns oppdrag(utbetaling(vedtakId = 1))
            every { vedtakId } returns VedtakId(1)
        }
    }


    private val inspector = TestRapid().apply {
        VedtakMottaker(
            rapidsConnection = this,
            utbetalingService = utbetalingService,
        )
    }

    @Test
    fun `attestert vedtak mottatt korrekt og utbetaling_oppdatert-event postet`() {
        every { utbetalingService.eksisterendeData(any()) } returns EksisterendeData(Datatype.INGEN_EKSISTERENDE_DATA)
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_oppdatert", get("@event_name").textValue())
            assertEquals(1L, get("@vedtakId").longValue())
            assertEquals("SENDT", get("@status").textValue())
        }
    }

    @Test
    fun `mottatt vedtak eksisterer fra for og poster utbetaling eksisterer-melding`() {
        every { utbetalingService.eksisterendeData(any()) } returns EksisterendeData(Datatype.EKSISTERENDE_VEDTAKID)
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_eksisterer", get("@event_name").textValue())
            assertEquals(1L, get("@vedtakId").longValue())
        }
    }

    @Test
    fun `utbetalingslinjer i mottatt vedtak eksisterer fra for og poster utbetalingslinjer eksisterer-melding`() {
        every { utbetalingService.eksisterendeData(any()) } returns EksisterendeData(
            Datatype.EKSISTERENDE_UTBETALINGSLINJEID,
            listOf(1, 2, 3)
        )
        inspector.apply { sendTestMessage(FATTET_VEDTAK) }

        inspector.inspektør.message(0).run {
            assertEquals("utbetalingslinjer_eksisterer", get("@event_name").textValue())
        }
    }


    @Test
    fun `mottatt vedtak forer til feil og publiserer utbetaling feilet-melding`() {
        every { utbetalingService.eksisterendeData(any()) } throws Exception()
        assertThrows<Exception> { inspector.apply { sendTestMessage(FATTET_VEDTAK) } }

        inspector.inspektør.message(0).run {
            assertEquals("utbetaling_feilet", get("@event_name").textValue())
            assertEquals(1L, get("@vedtakId").longValue())
        }
    }

    companion object {
        val FATTET_VEDTAK = readFile("/vedtak.json")
    }
}