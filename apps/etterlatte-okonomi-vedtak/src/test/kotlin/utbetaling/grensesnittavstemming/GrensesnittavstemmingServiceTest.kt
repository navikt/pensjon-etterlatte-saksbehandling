package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.common.Tidspunkt
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
        val utbetalingsoppdrag = listOf(utbetaling(status = UtbetalingStatus.FEILET))

        val grensesnittavstemming = Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            fraOgMed = fraOgMed,
            til = til,
            antallAvstemteOppdrag = 10
        )

        every { grensesnittavstemmingDao.hentSisteAvstemming() } returns grensesnittavstemming
        every { utbetalingDao.hentAlleUtbetalingerMellom(any(), any()) } returns utbetalingsoppdrag
        every { avstemmingsdataSender.sendAvstemming(any()) } just runs
        every { grensesnittavstemmingDao.opprettAvstemming(any()) } returns 1

        grensesnittsavstemmingService.startGrensesnittsavstemming(fraOgMed, til)

        verify(exactly = 3) { avstemmingsdataSender.sendAvstemming(any()) }
        verify {
            grensesnittavstemmingDao.opprettAvstemming(match {
                it.antallAvstemteOppdrag == 1 && it.fraOgMed == fraOgMed
            })
        }
    }


}