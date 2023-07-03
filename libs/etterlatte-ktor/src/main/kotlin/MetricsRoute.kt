package no.nav.etterlatte.libs.ktor

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Route.metrics() {
    get("/metrics") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
        }
    }
}