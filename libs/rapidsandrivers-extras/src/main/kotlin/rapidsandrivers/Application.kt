package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.RapidsAndRiversAppBuilder

fun <A : RapidsAndRiversAppBuilder> startRapidApplication(
    appBuilder: (env: Miljoevariabler) -> A,
    also: (rc: RapidsConnection, service: A) -> Unit
) {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        appBuilder.invoke(Miljoevariabler(env)).also { ab ->
            RapidApplication.create(env)
                .also {
                    also.invoke(it, ab)
                }.start()
        }
    }
}