package no.nav.etterlatte.utbetaling.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
        val idag = LocalDate.now()
        every { leaderElection.isLeader() } returns true
        every { konsistensavstemmingService.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idag) } returns false
        every { konsistensavstemmingService.startKonsistensavstemming(any(), any()) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) { konsistensavstemmingService.startKonsistensavstemming(any(), Saktype.BARNEPENSJON) }
        Assertions.assertTrue(leaderElection.isLeader())
    }

    @Test
    fun `skal konsistensavstemme for barnepensjon naar jobb ikke er kjoert samme dag`() {
        val idag = LocalDate.now()
        every { leaderElection.isLeader() } returns true
        every { konsistensavstemmingService.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idag) } returns false
        every { konsistensavstemmingService.startKonsistensavstemming(idag, Saktype.BARNEPENSJON) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) { konsistensavstemmingService.startKonsistensavstemming(idag, Saktype.BARNEPENSJON) }
    }

    @Test
    fun `skal ikke konsistensavstemme for barnepensjon naar jobb er kjoert samme dag`() {
        val idag = LocalDate.now()
        every { leaderElection.isLeader() } returns true
        every { konsistensavstemmingService.konsistensavstemmingErKjoertIDag(Saktype.BARNEPENSJON, idag) } returns true
        every { konsistensavstemmingService.startKonsistensavstemming(idag, Saktype.BARNEPENSJON) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 0) { konsistensavstemmingService.startKonsistensavstemming(idag, Saktype.BARNEPENSJON) }
    }
}