package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics

import io.prometheus.client.CollectorRegistry
import no.nav.etterlatte.jobs.MetrikkUthenter
import org.slf4j.LoggerFactory

class VedtakMetrics(
    private val vedtakMetrikkerDao: VedtakMetrikkerDao,
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run() {
        TODO("Not yet implemented")
    }
}
