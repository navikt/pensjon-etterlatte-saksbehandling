package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class VedtakMetrics(
    private val vedtakMetrikkerDao: VedtakMetrikkerDao,
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val loependeVedtak by lazy {
        Gauge
            .build("etterlatte_vedtak_loepende", "Løpende vedtak for etterlatte ytelser")
            .labelNames("saktype")
            .register(registry)
    }

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        vedtakMetrikkerDao.hentLoependeYtelseAntall().forEach {
            loependeVedtak.labels(it.sakType.name).set(it.antall.toDouble())
        }
    }
}
