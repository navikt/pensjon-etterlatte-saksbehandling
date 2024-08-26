package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class VedtakMetrics(
    private val vedtakMetrikkerDao: VedtakMetrikkerDao,
    private val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        vedtakMetrikkerDao.hentLoependeYtelseAntall().forEach { tell(it) }
    }

    private fun tell(it: VedtakAntall) {
        Gauge
            .builder("etterlatte_vedtak_loepende", { it.antall.toDouble() })
            .description("Løpende vedtak for etterlatte ytelser")
            .tag("saktype", it.sakType.name)
            .register(registry)
    }
}
