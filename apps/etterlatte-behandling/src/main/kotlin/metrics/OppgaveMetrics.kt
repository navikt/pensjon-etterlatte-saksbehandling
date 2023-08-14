package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: MetrikkerDao) : MeterBinder {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private var antallOppgaver = -1.0
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("antall_oppgaver") {
            antallOppgaver.also {
                antallOppgaver = metrikkerDao.antallOppgaver()
                logger.info("Antall oppgaver: $antallOppgaver")
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