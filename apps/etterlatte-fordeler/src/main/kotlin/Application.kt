package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also {
            RapidApplication.create(env)
                .also { EtterlatteFordeler(it, AppBuilder(env).createPersonService(), FordelerKriterierService()) }
                .start()
        }
    }
}