package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.config.LeaderElection
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

internal class GrensesnittsavstemmingsJobTest {

    private val grensesnittavstemmingService: GrensesnittsavstemmingService = mockk {
        every { hentNestePeriode() } returns Avstemmingsperiode(
            Tidspunkt.now().minus(1, ChronoUnit.DAYS),
            Tidspunkt.now()
        )
    }
    private val leaderElection: LeaderElection = mockk()
    private val grensesnittavstemming = GrensesnittsavstemmingJob.Grensesnittsavstemming(
        grensesnittsavstemmingService = grensesnittavstemmingService,
        leaderElection = leaderElection,
        jobbNavn = "jobb"
    )

    @Test
    fun `skal ikke grensesnittsavstemme siden pod ikke er leader`() {
        every { leaderElection.isLeader() } returns false

        grensesnittavstemming.run()

        verify(exactly = 0) { grensesnittavstemmingService.startGrensesnittsavstemming() }
        assertFalse(leaderElection.isLeader())
    }

    @Test
    fun `skal grensesnittsavstemme siden pod er leader`() {
        every { leaderElection.isLeader() } returns true
        every { grensesnittavstemmingService.startGrensesnittsavstemming(any()) } returns Unit

        grensesnittavstemming.run()

        verify(exactly = 1) { grensesnittavstemmingService.startGrensesnittsavstemming(any()) }
        assertTrue(leaderElection.isLeader())
    }
}