package no.nav.etterlatte.libs.ktor

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Application.metricsModule() {
    val collectorRegistry = CollectorRegistry.defaultRegistry

    val registrySaks = PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT,
        collectorRegistry,
        Clock.SYSTEM
    )

    install(MicrometerMetrics) {
        registry = registrySaks
        meterBinders = listOf(
            io.micrometer.core.instrument.binder.logging.LogbackMetrics(),
            io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics(),
            io.micrometer.core.instrument.binder.system.ProcessorMetrics(),
            io.micrometer.core.instrument.binder.system.UptimeMetrics(),
            io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics(),
            io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics(),
            io.micrometer.core.instrument.binder.jvm.JvmGcMetrics(),
            io.micrometer.core.instrument.binder.system.ProcessorMetrics(),
            io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics(),
            io.micrometer.core.instrument.binder.system.FileDescriptorMetrics()
        )
    }

    routing {
        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }
    }
}