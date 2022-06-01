package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.tidspunktMidnattIdag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class GrensesnittavstemmingServiceTest {

    private val grensesnittavstemmingDao: GrensesnittavstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()
    private val clock: Clock = Clock.systemUTC()

    private val grensesnittavstemmingService: GrensesnittsavstemmingService = GrensesnittsavstemmingService(
        avstemmingsdataSender = avstemmingsdataSender,
        grensesnittavstemmingDao = grensesnittavstemmingDao,
        utbetalingDao = utbetalingDao,
        clock = clock
    )

    @Test
    fun `skal opprette avstemming og sende til oppdrag`() {
        val periode = Avstemmingsperiode(
            fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS)),
            til = Tidspunkt.now()
        )
        val utbetaling = listOf(utbetaling(status = UtbetalingStatus.FEILET))

        val grensesnittavstemming = Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            periode = periode,
            antallOppdrag = 10,
            avstemmingsdata = ""
        )

        every { grensesnittavstemmingDao.hentSisteAvstemming() } returns grensesnittavstemming
        every { utbetalingDao.hentUtbetalinger(any(), any()) } returns utbetaling
        every { avstemmingsdataSender.sendAvstemming(any()) } returns "message"
        every { grensesnittavstemmingDao.opprettAvstemming(any()) } returns 1

        grensesnittavstemmingService.startGrensesnittsavstemming(periode)

        verify(exactly = 3) { avstemmingsdataSender.sendAvstemming(any()) }
        verify {
            grensesnittavstemmingDao.opprettAvstemming(match {
                it.antallOppdrag == 1 && it.periode.fraOgMed == periode.fraOgMed
            })
        }
    }

    @Test
    fun `skal kaste feil dersom fraOgMed tidspunkt ikke er stoerre enn til tidspunkt i avstemmingsperiode`() {
        val midnattIdag = tidspunktMidnattIdag(clock)

        every { grensesnittavstemmingDao.hentSisteAvstemming() } returns Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            periode = Avstemmingsperiode(
                fraOgMed = midnattIdag.minus(1, ChronoUnit.DAYS),
                til = midnattIdag
            ),
            antallOppdrag = 1,
            avstemmingsdata = "",
        )

        assertThrows<IllegalArgumentException> {
            grensesnittavstemmingService.hentNestePeriode()
        }
    }
}
