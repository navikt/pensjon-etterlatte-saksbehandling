package no.nav.etterlatte.libs.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry

fun Application.metricsModule() {
    val collectorRegistry = CollectorRegistry.defaultRegistry

    val registrySaksbehandling = PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT,
        collectorRegistry,
        Clock.SYSTEM
    )

    install(MicrometerMetrics) {
        registry = registrySaksbehandling
        meterBinders = listOf(
            LogbackMetrics(),
            JvmMemoryMetrics(),
            ProcessorMetrics(),
            UptimeMetrics()
        )
    }

    routing {
        get("/metrics") {
            call.respond(registrySaksbehandling.scrape())
        }
    }
}