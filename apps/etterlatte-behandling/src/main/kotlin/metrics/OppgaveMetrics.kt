package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.etterlatte.inTransaction
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: MetrikkerDao) : MeterBinder {

    private val logger = LoggerFactory.getLogger(this::class.java)
    override fun bindTo(registry: MeterRegistry) {
        val antallOppgaver = inTransaction {
            metrikkerDao.antallOppgaver()
        }
        logger.info("Antall oppgaver: $antallOppgaver")
        
        Gauge.builder("antall_oppgaver") {
            antallOppgaver
        }.description("Hello antall oppgaver")
            .register(registry)
        Counter.builder("hvor_fine_er_oppgavene")
            .apply {
                description("hello counter test")
                register(registry)
            }
    }
}