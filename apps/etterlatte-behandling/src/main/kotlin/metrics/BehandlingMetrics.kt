package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class BehandlingMetrics(
    private val oppgaveMetrikkerDao: OppgaveMetrikkerDao,
    private val behandlingerMetrikkerDao: BehandlingMetrikkerDao,
    private val gjenopprettingDao: GjenopprettingMetrikkerDao,
    private val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        oppgaveMetrikkerDao.hentOppgaveAntall().forEach {
            Gauge
                .builder("etterlatte oppgaver") { it.antall.toDouble() }
                .description("Antall oppgaver")
                .tag("status", it.status.name)
                .tag("enhet", it.enhet)
                .tag("saktype", it.saktype.name)
                .register(registry)
        }
        oppgaveMetrikkerDao.hentDistinkteSaksbehandlere().forEach {
            Gauge
                .builder("etterlatte_oppgaver_saksbehandler") { it.antall.toDouble() }
                .description("Antall saksbehandlere per enhet")
                .tag("enhet", it.enhet)
                .register(registry)
        }

        behandlingerMetrikkerDao.hent().forEach {
            Gauge
                .builder("etterlatte_behandlinger") { it.antall.toDouble() }
                .description("Antall behandlinger")
                .tag("saktype", it.saktype.name)
                .tag("behandlingstype", it.behandlingstype.name)
                .tag("status", it.status.name)
                .tag("revurdering_aarsak", it.revurderingsaarsak?.name ?: "null")
                .tag("kilde", it.kilde.name)
                .tag("automatiskMigrering", it.automatiskMigrering)
                .register(registry)
        }

        gjenopprettingDao.gjenopprettinger().forEach {
            Gauge
                .builder("etterlatte_gjenopprettinger") { it.antall.toDouble() }
                .description("Antall gjenopprettinger")
                .tag("automatisk", it.automatisk)
                .tag("status", it.status.name)
                .tag("type", it.type)
                .tag("enhet", it.enhet)
                .register(registry)
        }

        gjenopprettingDao.avbruttGrunnetSoeknad().forEach {
            Gauge
                .builder("etterlatte_gjenopprettinger_soeknad") { it.toDouble() }
                .description("Antall gjenopprettinger avbrutt på grunn av søknad")
                .register(registry)
        }

        gjenopprettingDao.over20().let {
            Gauge
                .builder("etterlatte_gjenopprettinger_over_20") { it.size.toDouble() }
                .description("Alle iverksatte saker med søker over 20 uten opphør")
                .register(registry)
        }
    }
}
