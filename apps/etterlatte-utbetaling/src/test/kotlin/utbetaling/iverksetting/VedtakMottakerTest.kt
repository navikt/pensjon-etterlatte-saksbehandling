package no.nav.etterlatte.utbetaling.iverksetting

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingslinje
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtakMottakerTest {

    private val utbetalingService = mockk<UtbetalingService>()
    private val inspector = TestRapid().apply {
        VedtakMottaker(
            rapidsConnection = this,
            utbetalingService = utbetalingService,
        )
    }

    @Test
    fun `skal returnere event med SENDT status dersom utbetaling sendes til oppdrag`() {
        val utbetaling = utbetaling(status = UtbetalingStatus.SENDT, vedtakId = 1)

        every { utbetalingService.iverksettUtbetaling(any()) } returns IverksettResultat.SendtTilOppdrag(utbetaling)

        inspector.apply { sendTestMessage(ATTESTERT_VEDTAK) }

        inspector.inspektør.message(0).run {
            val event = objectMapper.readValue(this.toJson(), UtbetalingEvent::class.java)
            assertEquals(EVENT_NAME_OPPDATERT, event.event)
            assertEquals(utbetaling.status, event.utbetalingResponse.status)
            assertEquals(utbetaling.vedtakId.value, event.utbetalingResponse.vedtakId)
        }
    }

    @Test
    fun `skal returnere event med FEILET status og feilmelding dersom utbetaling for vedtak eksisterer`() {
        val utbetaling = utbetaling(status = UtbetalingStatus.GODKJENT, vedtakId = 1)

        every { utbetalingService.iverksettUtbetaling(any()) } returns
                IverksettResultat.UtbetalingForVedtakEksisterer(utbetaling)

        inspector.apply { sendTestMessage(ATTESTERT_VEDTAK) }

        inspector.inspektør.message(0).run {
            val event = objectMapper.readValue(this.toJson(), UtbetalingEvent::class.java)
            assertEquals(EVENT_NAME_OPPDATERT, event.event)
            assertEquals(UtbetalingStatus.FEILET, event.utbetalingResponse.status)
            assertEquals(utbetaling.vedtakId.value, event.utbetalingResponse.vedtakId)
            assertEquals(
                "Vedtak med vedtakId=${utbetaling.vedtakId.value} eksisterer fra før",
                event.utbetalingResponse.feilmelding
            )
        }
    }

    @Test
    fun `skal returnere event med FEILET status og feilmelding dersom utbetalinglinjer for vedtak eksisterer`() {
        val utbetalingslinjer = listOf(
            utbetalingslinje(utbetalingslinjeId = 1),
            utbetalingslinje(utbetalingslinjeId = 2)
        )

        every { utbetalingService.iverksettUtbetaling(any()) } returns
                IverksettResultat.UtbetalingslinjerForVedtakEksisterer(utbetalingslinjer)

        inspector.apply { sendTestMessage(ATTESTERT_VEDTAK) }

        inspector.inspektør.message(0).run {
            val event = objectMapper.readValue(this.toJson(), UtbetalingEvent::class.java)
            assertEquals(EVENT_NAME_OPPDATERT, event.event)
            assertEquals(UtbetalingStatus.FEILET, event.utbetalingResponse.status)
            assertEquals(1, event.utbetalingResponse.vedtakId)
            assertEquals(
                "En eller flere utbetalingslinjer med id=[1,2] eksisterer fra før",
                event.utbetalingResponse.feilmelding
            )
        }
    }

    @Test
    fun `skal returnere event med FEILET status og generell feilmelding dersom en uventet feil oppstar`() {
        every { utbetalingService.iverksettUtbetaling(any()) } throws RuntimeException("Noe feilet")
        assertThrows<RuntimeException> { inspector.apply { sendTestMessage(ATTESTERT_VEDTAK) } }

        inspector.inspektør.message(0).run {
            val event = objectMapper.readValue(this.toJson(), UtbetalingEvent::class.java)
            assertEquals(EVENT_NAME_OPPDATERT, event.event)
            assertEquals(UtbetalingStatus.FEILET, event.utbetalingResponse.status)
            assertEquals(1, event.utbetalingResponse.vedtakId)
            assertEquals(
                "En feil oppstod under prosessering av vedtak med vedtakId=1: Noe feilet",
                event.utbetalingResponse.feilmelding
            )
        }
    }

    companion object {
        val ATTESTERT_VEDTAK = readFile("/vedtak.json")
    }
}