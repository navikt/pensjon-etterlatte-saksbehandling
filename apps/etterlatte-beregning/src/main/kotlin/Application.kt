package no.nav.etterlatte

import LesBeregningsmelding
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        RapidApplication.create(env)
            .also { LesBeregningsmelding(it, BeregningService()) }
            .start()
    }
}