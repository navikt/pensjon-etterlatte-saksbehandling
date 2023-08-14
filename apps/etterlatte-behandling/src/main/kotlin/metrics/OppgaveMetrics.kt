package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.etterlatte.inTransaction

class OppgaveMetrics(private val metrikkerDao: MetrikkerDao) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("antall_oppgaver") {
            inTransaction {
                metrikkerDao.antallOppgaver()
            }
        }.description("Hello antall oppgaver")
            .register(registry)
        Counter.builder("hvor_fine_er_oppgavene")
            .apply {
                description("hello counter test")
                register(registry)
            }
    }
}