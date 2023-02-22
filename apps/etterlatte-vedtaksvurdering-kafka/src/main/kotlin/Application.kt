package no.nav.etterlatte

import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoersel
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env).also { LoependeYtelserforespoersel(it, ab.lagVedtakKlient()) }.start()
        }
    }
}