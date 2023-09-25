package no.nav.etterlatte.libs.common.behandling

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class EtterbetalingTest {
    @Test
    fun `er ikke etterbetaling for virkningsdato fram i tid`() {
        val virkningsdato = LocalDate.now().plusDays(1)
        assertFalse(erEtterbetaling(virkningsdato = virkningsdato, now = LocalDate.now()))
    }

    @Test
    fun `er etterbetaling for virkningsdato i forrige maaned`() {
        val now = LocalDate.now()
        val virkningsdato = now.minusMonths(1)
        assertTrue(erEtterbetaling(virkningsdato = virkningsdato, now = now))
    }

    @Test
    fun `er etterbetaling for virkningsdato foer passert utbetalingsdag i samme maaned naar naa er etter`() {
        val utbetalingsdag = LocalDate.of(2023, Month.SEPTEMBER, UTBETALINGSDAG)
        val virkningsdato = utbetalingsdag.minusDays(2)
        assertTrue(erEtterbetaling(virkningsdato = virkningsdato, now = utbetalingsdag.plusDays(2)))
    }

    @Test
    fun `er ikke etterbetaling for virkningsdato foer passert utbetalingsdag i samme maaned naar naa er foer`() {
        val utbetalingsdag = LocalDate.of(2023, Month.SEPTEMBER, UTBETALINGSDAG)
        val virkningsdato = utbetalingsdag.minusDays(2)
        assertFalse(erEtterbetaling(virkningsdato = virkningsdato, now = utbetalingsdag.minusDays(1)))
    }

    @Test
    fun `er etterbetaling for virkningsdato paa utbetalingsdag`() {
        val virkningsdato = LocalDate.of(2023, Month.SEPTEMBER, UTBETALINGSDAG)
        assertTrue(erEtterbetaling(virkningsdato = virkningsdato, now = virkningsdato.plusDays(1)))
    }
}
