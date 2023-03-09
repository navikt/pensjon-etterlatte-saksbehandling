package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.utbetaling.common.tidspunktMidnattIdag
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.temporal.ChronoUnit

internal class GrensesnittavstemmingServiceTest {

    private val avstemmingDao: AvstemmingDao = mockk()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val avstemmingsdataSender: AvstemmingsdataSender = mockk()
    private val clock: Clock = utcKlokke()

    private val grensesnittavstemmingService: GrensesnittsavstemmingService = GrensesnittsavstemmingService(
        avstemmingsdataSender = avstemmingsdataSender,
        avstemmingDao = avstemmingDao,
        utbetalingDao = utbetalingDao,
        clock = clock
    )

    @Test
    fun `skal opprette avstemming og sende til oppdrag`() {
        val periode = Avstemmingsperiode(
            fraOgMed = Tidspunkt.now().minus(1, ChronoUnit.DAYS),
            til = Tidspunkt.now()
        )
        val utbetaling =
            listOf(utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))))

        val grensesnittavstemming = Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            periode = periode,
            antallOppdrag = 10,
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        every {
            avstemmingDao.hentSisteGrensesnittavstemming(saktype = Saktype.BARNEPENSJON)
        } returns grensesnittavstemming

        every {
            utbetalingDao.hentUtbetalingerForGrensesnittavstemming(
                any(),
                any(),
                Saktype.BARNEPENSJON
            )
        } returns utbetaling
        every { avstemmingsdataSender.sendGrensesnittavstemming(any()) } returns "message"
        every { avstemmingDao.opprettGrensesnittavstemming(any()) } returns 1

        grensesnittavstemmingService.startGrensesnittsavstemming(Saktype.BARNEPENSJON, periode)

        verify(exactly = 3) { avstemmingsdataSender.sendGrensesnittavstemming(any()) }
        verify {
            avstemmingDao.opprettGrensesnittavstemming(
                match {
                    it.antallOppdrag == 1 && it.periode.fraOgMed == periode.fraOgMed
                }
            )
        }
    }

    @Test
    fun `skal kaste feil dersom fraOgMed tidspunkt ikke er stoerre enn til tidspunkt i avstemmingsperiode`() {
        val midnattIdag = tidspunktMidnattIdag(clock)

        every { avstemmingDao.hentSisteGrensesnittavstemming(Saktype.BARNEPENSJON) } returns Grensesnittavstemming(
            opprettet = Tidspunkt.now(),
            periode = Avstemmingsperiode(
                fraOgMed = midnattIdag.minus(1, ChronoUnit.DAYS),
                til = midnattIdag
            ),
            antallOppdrag = 1,
            avstemmingsdata = "",
            saktype = Saktype.BARNEPENSJON
        )

        assertThrows<IllegalArgumentException> {
            grensesnittavstemmingService.hentNestePeriode(Saktype.BARNEPENSJON)
        }
    }
}