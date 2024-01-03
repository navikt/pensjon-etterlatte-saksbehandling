package no.nav.etterlatte.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class BehandlingMetrics(
    private val oppgaveMetrikkerDao: OppgaveMetrikkerDao,
    private val behandlingerMetrikkerDao: BehandlingMetrikkerDao,
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val oppgaver by lazy {
        Gauge.build("etterlatte_oppgaver", "Antall oppgaver")
            .labelNames("status", "enhet", "saktype")
            .register(registry)
    }

    val saksbehandler by lazy {
        Gauge.build("etterlatte_oppgaver_saksbehandler", "Antall saksbehandlere per enhet")
            .labelNames("enhet")
            .register(registry)
    }

    val behandlinger by lazy {
        Gauge.build("etterlatte_behandlinger", "Antall behandlinger")
            .labelNames("saktype", "behandlingstype", "status", "revurdering_aarsak", "kilde", "automatiskMigrering")
            .register(registry)
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        oppgaveMetrikkerDao.hentOppgaveAntall().forEach {
            oppgaver.labels(it.status.name, it.enhet, it.saktype.name).set(it.antall.toDouble())
        }
        oppgaveMetrikkerDao.hentDistinkteSaksbehandlere().forEach {
            saksbehandler.labels(it.enhet).set(it.antall.toDouble())
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
