package no.nav.etterlatte.metrics

import io.prometheus.client.Gauge
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class BehandlingMetrics(
    private val metrikkerDao: OppgaveMetrikkerDao,
    private val behandlingerMetrikkerDao: BehandlingMetrikkerDao,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val oppgaver by lazy {
        Gauge.build("etterlatte_oppgaver", "Antall oppgaver")
            .labelNames("status", "enhet", "saktype")
            .register()
    }

    val behandlinger by lazy {
        Gauge.build("etterlatte_behandlinger", "Antall behandlinger")
            .labelNames("saktype", "behandlingstype", "status", "revurdering_aarsak", "kilde", "automatiskMigrering")
            .register()
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        metrikkerDao.hentOppgaveAntall().forEach {
            oppgaver.labels(it.status.name, it.enhet, it.saktype.name).set(it.antall.toDouble())
        }

        behandlingerMetrikkerDao.hent().forEach {
            behandlinger.labels(
                it.saktype.name,
                it.behandlingstype.name,
                it.status.name,
                it.revurderingsaarsak?.name ?: "null",
                it.kilde.name,
                it.automatiskMigrering,
            ).set(it.antall.toDouble())
        }
    }
}
