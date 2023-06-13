package no.nav.etterlatte.utbetaling.grensesnittavstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

internal class GrensesnittsavstemmingsJobTest {

    private val grensesnittavstemmingService: GrensesnittsavstemmingService = mockk {
        every { hentNestePeriode(saktype = Saktype.BARNEPENSJON) } returns Avstemmingsperiode(
            Tidspunkt.now().minus(1, ChronoUnit.DAYS),
            Tidspunkt.now()
        )
        every { hentNestePeriode(saktype = Saktype.OMSTILLINGSSTOENAD) } returns Avstemmingsperiode(
            Tidspunkt.now().minus(1, ChronoUnit.DAYS),
            Tidspunkt.now()
        )
    }
    private val leaderElection: LeaderElection = mockk()
    private val grensesnittavstemming = GrensesnittsavstemmingJob.Grensesnittsavstemming(
        grensesnittsavstemmingService = grensesnittavstemmingService,
        leaderElection = leaderElection,
        jobbNavn = "jobb",
        saktype = Saktype.BARNEPENSJON
    )
    private val OMSgrensesnittavstemming = GrensesnittsavstemmingJob.Grensesnittsavstemming(
        grensesnittsavstemmingService = grensesnittavstemmingService,
        leaderElection = leaderElection,
        jobbNavn = "jobb",
        saktype = Saktype.OMSTILLINGSSTOENAD
    )

    @Test
    fun `skal ikke grensesnittsavstemme siden pod ikke er leader`() {
        every { leaderElection.isLeader() } returns false

        grensesnittavstemming.run()

        verify(exactly = 0) { grensesnittavstemmingService.startGrensesnittsavstemming(any()) }
        assertFalse(leaderElection.isLeader())
    }

    @Test
    fun `skal grensesnittsavstemme siden pod er leader`() {
        every { leaderElection.isLeader() } returns true
        every { grensesnittavstemmingService.startGrensesnittsavstemming(saktype = Saktype.BARNEPENSJON) } returns Unit
        every { grensesnittavstemmingService.startGrensesnittsavstemming(saktype = Saktype.OMSTILLINGSSTOENAD) } returns Unit

        grensesnittavstemming.run()
        OMSgrensesnittavstemming.run()

        verify(exactly = 1) {
            grensesnittavstemmingService.startGrensesnittsavstemming(saktype = Saktype.BARNEPENSJON)
        }
        verify(exactly = 1) {
            grensesnittavstemmingService.startGrensesnittsavstemming(saktype = Saktype.OMSTILLINGSSTOENAD)
        }
        assertTrue(leaderElection.isLeader())
    }
}