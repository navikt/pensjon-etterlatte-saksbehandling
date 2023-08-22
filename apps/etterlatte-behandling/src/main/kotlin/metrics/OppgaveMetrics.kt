package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import java.time.Duration
import java.time.Instant

class OppgaveMetrics(private val metrikkerDao: MetrikkerDao) : MeterBinder {

    private var oppgaverPerStatusCache: List<Pair<Status, Long>> = emptyList()
    private var sistHentetOppgaverMedStatus: Instant = Instant.EPOCH
    private var oppgaverGauge: MultiGauge? = null

    private val maxTidCacheOppgaver = Duration.ofSeconds(60)

    override fun bindTo(registry: MeterRegistry) {
        oppgaverGauge = MultiGauge.builder("etterlatte_oppgaver")
            .description("Antall oppgaver, etter status")
            .register(registry)

        Gauge.builder("etterlatte_oppgaver_oppdatert", this) {
            oppgaverGauge?.register(
                this.oppgaverPerStatus().map { (status, antall) ->
                    MultiGauge.Row.of(Tags.of("status", status.name), antall)
                }
            )
            sistHentetOppgaverMedStatus.epochSecond.toDouble()
        }.description("Epochsecond for siste uthenting av oppgaver")
            .register(registry)
    }

    private fun oppgaverPerStatus(): List<Pair<Status, Long>> {
        if (sistHentetOppgaverMedStatus.plus(maxTidCacheOppgaver).isBefore(Instant.now())) {
            oppgaverPerStatusCache = metrikkerDao.hentOppgaverMedStatus()
            sistHentetOppgaverMedStatus = Instant.now()
        }
        return oppgaverPerStatusCache
    }
}