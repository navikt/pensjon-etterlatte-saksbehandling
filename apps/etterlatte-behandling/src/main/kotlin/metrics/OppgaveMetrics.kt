package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: OppgaveMetrikkerDao) : MeterBinder {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun bindTo(registry: MeterRegistry) {
        val oppgaveAntall = metrikkerDao.hentOppgaveAntall()

        Gauge.builder("antall_oppgaver", -1) { oppgaveAntall.totalt.toDouble() }
            .description("Antall oppgaver")
            .register(registry)

        Gauge.builder("antall_aktive_oppgaver", -1) { oppgaveAntall.aktive.toDouble() }
            .description("Antall aktive oppgaver")
            .register(registry)

        Gauge.builder("antall_avslutta_oppgaver", -1) { oppgaveAntall.avsluttet.toDouble() }
            .description("Antall avslutta oppgaver")
            .register(registry)
    }
}
