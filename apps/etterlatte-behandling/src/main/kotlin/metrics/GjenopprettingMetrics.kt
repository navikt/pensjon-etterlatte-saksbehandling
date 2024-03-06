package no.nav.etterlatte.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class GjenopprettingMetrics(
    private val dao: GjenopprettingMetrikkerDao,
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val gjenopprettinger by lazy {
        Gauge.build("etterlatte_gjenopprettinger", "Antall gjenopprettinger")
            .labelNames("automatisk", "status", "type", "enhet")
            .register(registry)
    }

    val avbruttGrunnetSoeknad by lazy {
        Gauge.build("etterlatte_gjenopprettinger_soeknad", "Antall gjenopprettinger avbrutt på grunn av søknad")
            .register(registry)
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        dao.gjenopprettinger().forEach {
            gjenopprettinger.labels(it.automatisk, it.status.name, it.type, it.enhet).set(it.antall.toDouble())
        }

        dao.avbruttGrunnetSoeknad().forEach {
            avbruttGrunnetSoeknad.labels().set(it.toDouble())
        }
    }
}
