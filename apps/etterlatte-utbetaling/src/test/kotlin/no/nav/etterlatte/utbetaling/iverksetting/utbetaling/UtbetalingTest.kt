package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.UUID

internal class UtbetalingTest {
    @Test
    fun `Utbetalingstatus for utbetaling skal vaere GODKJENT`() {
        val utbetalingshendelser =
            listOf(
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.GODKJENT,
                ),
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.GODKJENT_MED_FEIL,
                ),
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.AVVIST,
                ),
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.FEILET,
                ),
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.SENDT,
                ),
                utbetalingshendelse(
                    utbetalingId = UUID.randomUUID(),
                    status = UtbetalingStatus.MOTTATT,
                ),
            )
        val utbetalinger = utbetaling(utbetalingshendelser = utbetalingshendelser)

        assertEquals(UtbetalingStatus.GODKJENT, utbetalinger.status())
    }

    @Test
    fun `Utbetalingstatuser skal ha korrekt ordinalitet`() {
        assertAll(
            "Utbetalingstatuser skal ha korrekt ordinalitet",
            { assertEquals(0, UtbetalingStatus.GODKJENT.ordinal) },
            { assertEquals(1, UtbetalingStatus.GODKJENT_MED_FEIL.ordinal) },
            { assertEquals(2, UtbetalingStatus.AVVIST.ordinal) },
            { assertEquals(3, UtbetalingStatus.FEILET.ordinal) },
            { assertEquals(4, UtbetalingStatus.SENDT.ordinal) },
            { assertEquals(5, UtbetalingStatus.MOTTATT.ordinal) },
        )
    }

    @Test
    fun `utbetaling uten utbetalingshendelser skal ha status MOTTATT`() {
        val utbetalinger = utbetaling(utbetalingshendelser = emptyList())
        assertEquals(UtbetalingStatus.MOTTATT, utbetalinger.status())
    }
}
