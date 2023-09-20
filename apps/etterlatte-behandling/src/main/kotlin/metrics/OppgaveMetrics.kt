package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: OppgaveMetrikkerDao) : MeterBinder {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var antallOppgaver = -1.0
    private var antallAktiveOppgaver = -1.0
    private var antallAvsluttaOppgaver = -1.0

    override fun bindTo(registry: MeterRegistry) {
        val oppgaveAntall = metrikkerDao.hentOppgaveAntall()

        Gauge.builder("antall_oppgaver") {
            antallOppgaver.also {
                antallOppgaver = oppgaveAntall.totalt.toDouble()
                logger.info("Antall oppgaver: $antallOppgaver")
            }
        }.description("Antall oppgaver")
            .register(registry)

        Gauge.builder("antall_aktive_oppgaver") {
            antallAktiveOppgaver.also {
                antallAktiveOppgaver = oppgaveAntall.aktive.toDouble()
                logger.info("Antall aktive oppgaver: $antallAktiveOppgaver")
            }
        }.description("Antall aktive oppgaver")
            .register(registry)

        Gauge.builder("antall_avslutta_oppgaver") {
            antallAvsluttaOppgaver.also {
                antallAvsluttaOppgaver = oppgaveAntall.avsluttet.toDouble()
                logger.info("Antall avslutta oppgaver: $antallAvsluttaOppgaver")
            }
        }.description("Antall avslutta oppgaver")
            .register(registry)
    }
}
