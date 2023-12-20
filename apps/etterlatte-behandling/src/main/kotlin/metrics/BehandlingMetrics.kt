package no.nav.etterlatte.metrics

import io.prometheus.client.Gauge
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class BehandlingMetrics(
    private val metrikkerDao: OppgaveMetrikkerDao,
    private val behandlingerMetrikkerDao: BehandlingMetrikkerDao,
) : MetrikkUthenter {
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

    val behandlinger by lazy {
        Gauge.build("etterlatte_behandlinger", "Antall behandlinger")
            .labelNames("kilde", "status", "automatiskMigrering").register()
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        with(metrikkerDao.hentOppgaveAntall()) {
            antallOppgaver.set(totalt.toDouble())
            antallOppgaverAktive.set(aktive.toDouble())
            antallOppgaverAvslutta.set(avsluttet.toDouble())
        }

        behandlingerMetrikkerDao.hent().forEach {
            behandlinger.labels(
                it.kilde.name,
                it.status.name,
                it.automatiskMigrering,
            ).set(it.antall.toDouble())
        }
    }
}
