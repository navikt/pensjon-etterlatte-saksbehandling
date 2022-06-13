package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also { OppdaterBehandling(it, ab.createBehandlingService()) }
                .also { HendelserOmVedtak(it, ab.createBehandlingService()) }
                .also { TestHendelser(it, ab.createBehandlingService()) }
                .start()
        }
    }
}