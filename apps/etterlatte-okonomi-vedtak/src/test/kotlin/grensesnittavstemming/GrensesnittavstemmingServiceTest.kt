package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.domain.UtbetalingStatus
import no.nav.etterlatte.grensesnittavstemming.AvstemmingsdataSender
import no.nav.etterlatte.grensesnittavstemming.Grensesnittavstemming
import no.nav.etterlatte.grensesnittavstemming.GrensesnittavstemmingDao
import no.nav.etterlatte.grensesnittavstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetalingsoppdrag
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class GrensesnittavstemmingServiceTest {

    private val grensesnittavstemmingDao: GrensesnittavstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()

    private val grensesnittsavstemmingService: GrensesnittsavstemmingService = GrensesnittsavstemmingService(
        avstemmingsdataSender = avstemmingsdataSender,
        grensesnittavstemmingDao = grensesnittavstemmingDao,
        utbetalingDao = utbetalingDao
    )

    @Test
    fun `skal opprette avstemming og sende til oppdrag`() {
        val fraOgMed = LocalDateTime.now().minusDays(1)
        val til = LocalDateTime.now()
        val utbetalingsoppdrag = listOf(utbetalingsoppdrag(status = UtbetalingStatus.FEILET))

        val grensesnittavstemming = Grensesnittavstemming(
            opprettet = LocalDateTime.now(),
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