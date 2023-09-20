package no.nav.etterlatte.fordeler

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.prometheus.client.Counter
import org.junit.jupiter.api.Test

internal class FordelerMetricLoggerTest {
    private val metricKriterieIkkeOppfylt = mockk<Counter>()
    private val metricFordelerStatus = mockk<Counter>()
    private val fordelerMetricLogger =
        FordelerMetricLogger(
            kriterierIkkeOppfyltMetric = metricKriterieIkkeOppfylt,
            fordelerStatusMetric = metricFordelerStatus,
        )

    @Test
    fun `skal logge metrikker for ikke oppfylte kriterier og fordelerstatus`() {
        every { metricKriterieIkkeOppfylt.labels(FordelerKriterie.BARN_ER_FOR_GAMMELT.name).inc() } just runs
        every { metricKriterieIkkeOppfylt.labels(FordelerKriterie.AVDOED_HAR_YRKESSKADE.name).inc() } just runs
        every { metricFordelerStatus.labels(FordelerStatus.IKKE_GYLDIG_FOR_BEHANDLING.name).inc() } just runs

        fordelerMetricLogger.logMetricIkkeFordelt(
            FordelerResultat.IkkeGyldigForBehandling(
                listOf(FordelerKriterie.BARN_ER_FOR_GAMMELT, FordelerKriterie.AVDOED_HAR_YRKESSKADE),
            ),
        )

        verify(exactly = 1) { metricKriterieIkkeOppfylt.labels(FordelerKriterie.BARN_ER_FOR_GAMMELT.name).inc() }
        verify(exactly = 1) { metricKriterieIkkeOppfylt.labels(FordelerKriterie.AVDOED_HAR_YRKESSKADE.name).inc() }
        verify(exactly = 1) { metricFordelerStatus.labels(FordelerStatus.IKKE_GYLDIG_FOR_BEHANDLING.name).inc() }
        confirmVerified()
    }

    @Test
    fun `skal logge metrikker for fordelerstatus nar soknad ikke er gyldig for behandling `() {
        every { metricFordelerStatus.labels(FordelerStatus.GYLDIG_FOR_BEHANDLING.name).inc() } just runs

        fordelerMetricLogger.logMetricFordelt()

        verify(exactly = 1) { metricFordelerStatus.labels(FordelerStatus.GYLDIG_FOR_BEHANDLING.name).inc() }
        confirmVerified()
    }
}
