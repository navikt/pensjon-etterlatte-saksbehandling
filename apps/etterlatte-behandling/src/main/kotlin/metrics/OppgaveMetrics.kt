package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: MetrikkerDao) : MeterBinder {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private var antallOppgaver = -1.0
    private var antallAktiveOppgaver = -1.0
    private var antallAvsluttaOppgaver = -1.0

    override fun bindTo(registry: MeterRegistry) {
        val alleOppgaver = metrikkerDao.hentAlleOppgaver()

        val aktive = alleOppgaver.filter { !it.erAvsluttet() }
        val avsluttet = alleOppgaver.filter { it.erAvsluttet() }

        Gauge.builder("antall_oppgaver") {
            antallOppgaver.also {
                antallOppgaver = alleOppgaver.size.toDouble()
                logger.info("Antall oppgaver: $antallOppgaver")
            }
        }.description("Antall oppgaver")
            .register(registry)

        Gauge.builder("antall_aktive_oppgaver") {
            antallAktiveOppgaver.also {
                antallAktiveOppgaver = aktive.size.toDouble()
                logger.info("Antall aktive oppgaver: $antallAktiveOppgaver")
            }
        }.description("Antall aktive oppgaver")
            .register(registry)

        Gauge.builder("antall_avslutta_oppgaver") {
            antallAvsluttaOppgaver.also {
                antallAvsluttaOppgaver = avsluttet.size.toDouble()
                logger.info("Antall avslutta oppgaver: $antallAvsluttaOppgaver")
            }
        }.description("Antall avslutta oppgaver")
            .register(registry)
    }

}