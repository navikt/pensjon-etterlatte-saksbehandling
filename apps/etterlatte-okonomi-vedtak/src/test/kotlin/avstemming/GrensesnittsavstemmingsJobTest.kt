package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test

internal class GrensesnittsavstemmingsJobTest {

    @Test
    fun `skal ikke grensesnittsavstemme siden pod ikke er leader`() {
        GrensesnittsavstemmingJob.Grensesnittsavstemming(
            grensesnittsavstemmingService = mockk(),
            leaderElection = mockk() {
                every { isLeader() } returns false
            },
            jobbNavn = "jobb"
        ).let {
            it.run()
            verify(exactly = 0) { it.grensesnittsavstemmingService.startGrensesnittsavstemming() }
            assertFalse(it.leaderElection.isLeader())
        }
    }

    @Test
    fun `skal grensesnittsavstemme siden pod er leader`() {
        GrensesnittsavstemmingJob.Grensesnittsavstemming(
            grensesnittsavstemmingService = mockk() {
                every { startGrensesnittsavstemming() } returns Unit
            },
            leaderElection = mockk() {
                every { isLeader() } returns true
            },
            jobbNavn = "jobb"
        ).let {
            it.run()
            verify(exactly = 1) { it.grensesnittsavstemmingService.startGrensesnittsavstemming() }
            assertTrue(it.leaderElection.isLeader())
        }
    }


}