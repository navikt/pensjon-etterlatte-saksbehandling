package utbetaling.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.config.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class KonsistensavstemmingJobTest {

    private val konsistensavstemmingService: KonsistensavstemmingService = mockk {
    }
    private val leaderElection: LeaderElection = mockk()
    private val konsistensavstemming = KonsistensavstemmingJob.Konsistensavstemming(
        konsistensavstemmingService = konsistensavstemmingService,
        leaderElection = leaderElection,
        jobbNavn = "jobb"
    )

    @Test
    fun `skal ikke konsistensavstemme siden pod ikke er leader`() {
        every { leaderElection.isLeader() } returns false

        konsistensavstemming.run()

        verify(exactly = 0) { konsistensavstemmingService.startKonsistensavstemming(any(), any()) }
        Assertions.assertFalse(leaderElection.isLeader())
    }

    @Test
    fun `skal konsistensavstemme for Barnepensjon siden pod er leader`() {
        every { leaderElection.isLeader() } returns true
        every { konsistensavstemmingService.startKonsistensavstemming(any(), any()) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) { konsistensavstemmingService.startKonsistensavstemming(any(), Saktype.BARNEPENSJON) }
        Assertions.assertTrue(leaderElection.isLeader())
    }
}