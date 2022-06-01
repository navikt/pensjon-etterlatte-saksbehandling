package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

internal class GrensesnittsavstemmingsJobTest {

    @Test
    fun `skal ikke grensesnittsavstemme siden pod ikke er leader`() {


        GrensesnittsavstemmingJob.Grensesnittsavstemming(
            grensesnittsavstemmingService = mockk() {
                every { hentNestePeriode() } returns Avstemmingsperiode(
                    Tidspunkt.now().minus(1, ChronoUnit.DAYS),
                    Tidspunkt.now()
                )
            },
            leaderElection = mockk() {
                every { isLeader() } returns false
            },
            jobbNavn = "jobb"
        ).let {
            it.run()
            verify(exactly = 0) {
                it.grensesnittsavstemmingService.startGrensesnittsavstemming(
                )
            }
            assertFalse(it.leaderElection.isLeader())
        }
    }

    @Test
    fun `skal grensesnittsavstemme siden pod er leader`() {
        GrensesnittsavstemmingJob.Grensesnittsavstemming(
            grensesnittsavstemmingService = mockk() {
                every { startGrensesnittsavstemming(any()) } returns Unit
                every { hentNestePeriode() } returns Avstemmingsperiode(
                    Tidspunkt.now().minus(1, ChronoUnit.DAYS),
                    Tidspunkt.now()
                )
            },
            leaderElection = mockk() {
                every { isLeader() } returns true
            },
            jobbNavn = "jobb"
        ).let {
            it.run()
            verify(exactly = 1) {
                it.grensesnittsavstemmingService.startGrensesnittsavstemming(any())
            }
            assertTrue(it.leaderElection.isLeader())
        }
    }


}