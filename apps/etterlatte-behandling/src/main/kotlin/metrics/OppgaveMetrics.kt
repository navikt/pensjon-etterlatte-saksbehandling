package no.nav.etterlatte.metrics

import io.prometheus.client.Gauge
import org.slf4j.LoggerFactory

class OppgaveMetrics(private val metrikkerDao: OppgaveMetrikkerDao) : MetrikkUthenter() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val antallOppgaver by lazy {
        Gauge.build("antall_oppgaver", "Antall oppgaver").register()
    }
    val antallOppgaverAktive by lazy {
        Gauge.build("antall_aktive_oppgaver", "Antall aktive oppgaver").register()
    }
    val antallOppgaverAvslutta by lazy {
        Gauge.build("antall_avslutta_oppgaver", "Antall avslutta oppgaver").register()
    }

    override fun run() {
        logger.info("Samler metrikker for etterlatte-behandling")
        with(metrikkerDao.hentOppgaveAntall()) {
            antallOppgaver.set(totalt.toDouble())
            antallOppgaverAktive.set(aktive.toDouble())
            antallOppgaverAvslutta.set(avsluttet.toDouble())
        }
    }
}
