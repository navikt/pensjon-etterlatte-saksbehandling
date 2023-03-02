package no.nav.etterlatte.utbetaling.avstemming

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.fixedNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.midnattNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.common.februar
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

internal class KonsistensavstemmingJobTest {

    private val konsistensavstemmingService: KonsistensavstemmingService = mockk {
    }
    private val leaderElection: LeaderElection = mockk()
    private val datoEksekvering = 1.februar(2023)
    private val konsistensavstemming = KonsistensavstemmingJob.Konsistensavstemming(
        konsistensavstemmingService = konsistensavstemmingService,
        kjoereplan = setOf(datoEksekvering),
        leaderElection = leaderElection,
        jobbNavn = "jobb",
        clock = datoEksekvering.midnattNorskTid().toTidspunkt().fixedNorskTid()
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
        every {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                datoEksekvering
            )
        } returns false
        every { konsistensavstemmingService.startKonsistensavstemming(any(), any()) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) { konsistensavstemmingService.startKonsistensavstemming(any(), Saktype.BARNEPENSJON) }
        Assertions.assertTrue(leaderElection.isLeader())
    }

    @Test
    fun `skal ikke konsistensavstemme for barnepensjon naar datoen ikke er en del av kjoereplan`() {
        every { leaderElection.isLeader() } returns true

        val dagForTestMinusFemDager = datoEksekvering.midnattNorskTid().minus(5, ChronoUnit.DAYS).toTidspunkt()

        val konsistensavstemming = KonsistensavstemmingJob.Konsistensavstemming(
            konsistensavstemmingService = konsistensavstemmingService,
            kjoereplan = setOf(datoEksekvering),
            leaderElection = leaderElection,
            jobbNavn = "jobb",
            clock = dagForTestMinusFemDager.fixedNorskTid()
        )

        konsistensavstemming.run()

        verify(exactly = 0) {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                datoEksekvering
            )
        }
        verify(exactly = 0) {
            konsistensavstemmingService.startKonsistensavstemming(
                datoEksekvering,
                Saktype.BARNEPENSJON
            )
        }
        confirmVerified(konsistensavstemmingService)
    }

    @Test
    fun `skal konsistensavstemme for barnepensjon naar datoen er en del av kjoereplan`() {
        every { leaderElection.isLeader() } returns true
        every {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                datoEksekvering
            )
        } returns false
        every { konsistensavstemmingService.startKonsistensavstemming(any(), any()) } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                any()
            )
        }
        verify(exactly = 1) { konsistensavstemmingService.startKonsistensavstemming(any(), Saktype.BARNEPENSJON) }
        confirmVerified(konsistensavstemmingService)
    }

    @Test
    fun `skal konsistensavstemme for barnepensjon naar jobb ikke er kjoert samme dag`() {
        every { leaderElection.isLeader() } returns true
        every {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                datoEksekvering
            )
        } returns false
        every {
            konsistensavstemmingService.startKonsistensavstemming(
                datoEksekvering,
                Saktype.BARNEPENSJON
            )
        } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 1) {
            konsistensavstemmingService.startKonsistensavstemming(
                datoEksekvering,
                Saktype.BARNEPENSJON
            )
        }
    }

    @Test
    fun `skal ikke konsistensavstemme for barnepensjon naar jobb er kjoert samme dag`() {
        every { leaderElection.isLeader() } returns true
        every {
            konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                Saktype.BARNEPENSJON,
                datoEksekvering
            )
        } returns true
        every {
            konsistensavstemmingService.startKonsistensavstemming(
                datoEksekvering,
                Saktype.BARNEPENSJON
            )
        } returns emptyList()

        konsistensavstemming.run()

        verify(exactly = 0) {
            konsistensavstemmingService.startKonsistensavstemming(
                datoEksekvering,
                Saktype.BARNEPENSJON
            )
        }
    }
}