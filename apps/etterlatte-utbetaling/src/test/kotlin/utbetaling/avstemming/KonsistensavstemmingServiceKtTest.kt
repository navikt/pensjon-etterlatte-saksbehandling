package utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.utbetaling.avstemming.gjeldendeLinjerForEnDato
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KonsistensavstemmingServiceKtTest {

    @Test
    fun `gjeldendelinjerForEnDato tar med linjer som er relevante for en gitt dato`() {
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = LocalDate.of(1998, 1, 1),
            opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        )
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = LocalDate.of(1998, 6, 1),
            opprettet = LocalDate.of(1998, 2, 1).minusDays(16).toTidspunkt()

        )
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = LocalDate.of(1997, 11, 1),
            opprettet = LocalDate.of(1998, 9, 1).toTidspunkt()
        )

        val linjer = listOf(linje1, linje2, linje3)

        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.now()), listOf(linje3))
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 2, 25)), listOf(linje1, linje2))
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 7, 25)), listOf(linje2))
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 1, 2)), listOf(linje1))
    }

    @Test
    fun `gjeldendeLinjerForEnDato håndterer opphør korrekt`() {
        val linje1 = utbetalingslinje(
            utbetalingslinjeId = 1,
            periodeFra = LocalDate.of(1998, 1, 1),
            opprettet = LocalDate.of(1998, 1, 1).minusDays(16).toTidspunkt()
        )
        val linje2 = utbetalingslinje(
            utbetalingslinjeId = 2,
            erstatter = 1,
            periodeFra = LocalDate.of(1998, 6, 1),
            opprettet = LocalDate.of(1998, 2, 1).minusDays(16).toTidspunkt()

        )
        val linje3 = utbetalingslinje(
            utbetalingslinjeId = 3,
            erstatter = 2,
            periodeFra = LocalDate.of(2000, 11, 1),
            opprettet = LocalDate.of(1998, 9, 1).toTidspunkt(),
            type = Utbetalingslinjetype.OPPHOER
        )

        val linjer = listOf(linje1, linje2, linje3)

        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.now()), emptyList<Utbetalingslinje>())
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 2, 25)), listOf(linje1, linje2))
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 7, 25)), listOf(linje2))
    }

    private fun LocalDate.toTidspunkt(): Tidspunkt = this.atStartOfDay().toTidspunkt(norskTidssone)
}