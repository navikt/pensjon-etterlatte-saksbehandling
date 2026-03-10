package no.nav.etterlatte.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.etterlatte.jobs.MetrikkUthenter
import no.nav.etterlatte.libs.ktor.Metrikker
import org.slf4j.LoggerFactory
import kotlin.text.toDouble

class VedtakMetrics(
    private val vedtakMetrikkerDao: VedtakMetrikkerDao,
    private val registry: PrometheusMeterRegistry = Metrikker.registrySaksbehandling,
) : MetrikkUthenter {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run() {
        logger.info("Samler metrikker med ${this::class.simpleName}")

        vedtakMetrikkerDao.hentLoependeYtelseAntall().forEach { tell(it) }
    }

    private fun tell(it: VedtakAntall) {
        Gauge
            .builder("etterlatte_vedtak_loepende") { it.antall.toDouble() }
            .description("Løpende vedtak for etterlatte ytelser")
            .tag("saktype", it.sakType.name)
            .register(registry)
    }
}
