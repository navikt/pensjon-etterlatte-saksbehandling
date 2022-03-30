package no.nav.etterlatte

import no.nav.etterlatte.attestering.VedtaksMottaker
import no.nav.helse.rapids_rivers.RapidApplication


fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
    }

    // starter app
    RapidApplication.create(env)
        .also {
            VedtaksMottaker(it)
        }.start()
}

private fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
