package utbetaling.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.avstemming.gjeldendeLinjerForEnDato
import no.nav.etterlatte.utbetaling.grensesnittavstemming.AvstemmingDao
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KonsistensavstemmingServiceKtTest {

    private val avstemmingDao: AvstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()

    @Test
    fun `skal starte konsistensavstemming for Barnepensjon`() {
        val utbetaling = utbetaling()
        every {
            utbetalingDao.hentUtbetalingerForKonsistensavstemming(
                any(),
                any(),
                Saktype.BARNEPENSJON
            )
        } returns listOf(utbetaling)
        every { avstemmingDao.opprettKonsistensavstemming(any()) } returns 1

        every { avstemmingsdataSender.sendKonsistensavstemming(any()) } returns "<xml>data</xml>"

        val service = KonsistensavstemmingService(
            utbetalingDao = utbetalingDao,
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender
        )

        val dag = LocalDate.of(2022, 10, 10)
        val saktype = Saktype.BARNEPENSJON
        val resultat = service.startKonsistensavstemming(dag, saktype)

        assertTrue(resultat.all { it == "<xml>data</xml>" })
        verify { utbetalingDao.hentUtbetalingerForKonsistensavstemming(any(), any(), Saktype.BARNEPENSJON) }
        verify { avstemmingsdataSender.sendKonsistensavstemming(any()) }
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter.
     * 1:   |-------->
     * 2:       |---->
     * 3: |---------->
     */
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

        /* Case 1: Naa: linje 3 er eneste gjeldende */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.now()), listOf(linje3))
        /* Case 2: Foer linje 3 er aktiv -> linje 1 og 2 er aktive */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 2, 25)), listOf(linje1, linje2))
        /* Case 3: Etter linje 2 har tatt over for linje 1, men foer linje 3 er aktiv -> Linje 2 er aktiv */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 7, 25)), listOf(linje2))
        /* Case 4: Foer linje 2 og 3 er aktive opprettet -> linje 1 er aktiv */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 1, 2)), listOf(linje1))
    }

    /**
     * Utbetalingslinjene er sendt inn paa ulike tidspunkter.
     * 1:   |-------->
     * 2:       |---->
     * 3:         | (OPPHOER)
     */
    @Test
    fun `gjeldendeLinjerForEnDato haandterer opphoer korrekt`() {
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

        /* Case 1: Naa: linje 3 har foert til opphoer -> ingen aktive utbetalingslinjer */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.now()), emptyList<Utbetalingslinje>())
        /* Case 2: Foer linje 2 er aktiv, og foer linje 3 er opprettet -> linje 1 og 2 er aktive */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 2, 25)), listOf(linje1, linje2))
        /* Case 3: Etter linje 1 er aktiv, kun linje 2 er aktiv, foer linje 3 er opprettet -> linje 2 er aktiv */
        assertEquals(gjeldendeLinjerForEnDato(linjer, LocalDate.of(1998, 7, 25)), listOf(linje2))
    }

    private fun LocalDate.toTidspunkt(): Tidspunkt = this.atStartOfDay().toTidspunkt(norskTidssone)
}