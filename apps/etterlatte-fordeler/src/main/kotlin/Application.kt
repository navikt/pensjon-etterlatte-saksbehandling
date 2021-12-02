package no.nav.etterlatte.prosess

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { appBuilder ->
            RapidApplication.create(env)
                .also {EtterlatteFordeler(it, AppBuilder(env).createPersonService()) }
                .start()
        }
    }
}