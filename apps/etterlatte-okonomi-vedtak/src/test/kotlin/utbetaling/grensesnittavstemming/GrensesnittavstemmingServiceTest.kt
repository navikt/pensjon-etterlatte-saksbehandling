package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class GrensesnittavstemmingServiceTest {

    private val grensesnittavstemmingDao: GrensesnittavstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()

    private val grensesnittsavstemmingService: GrensesnittsavstemmingService = GrensesnittsavstemmingService(
        avstemmingsdataSender = avstemmingsdataSender,
        grensesnittavstemmingDao = grensesnittavstemmingDao,
        utbetalingDao = utbetalingDao,
        clock = Clock.systemUTC()
    )

    @Test
    fun `skal opprette avstemming og sende til oppdrag`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()
        val periode = Avstemmingsperiode(fraOgMed, til)
        val utbetaling = listOf(utbetaling(status = UtbetalingStatus.FEILET))

        val grensesnittavstemming = Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            periodeFraOgMed = fraOgMed,
            periodeTil = til,
            antallOppdrag = 10
        )

        every { grensesnittavstemmingDao.hentSisteAvstemming() } returns grensesnittavstemming
        every { utbetalingDao.hentAlleUtbetalingerMellom(any(), any()) } returns utbetaling
        every { avstemmingsdataSender.sendAvstemming(any()) } returns "message"
        every { grensesnittavstemmingDao.opprettAvstemming(any()) } returns 1

        grensesnittsavstemmingService.startGrensesnittsavstemming(periode)

        verify(exactly = 3) { avstemmingsdataSender.sendAvstemming(any()) }
        verify {
            grensesnittavstemmingDao.opprettAvstemming(match {
                it.antallOppdrag == 1 && it.periodeFraOgMed == fraOgMed
            })
        }
    }
}
