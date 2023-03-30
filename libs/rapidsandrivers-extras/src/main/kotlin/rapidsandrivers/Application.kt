package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.RapidsAndRiversAppBuilder

fun <A : RapidsAndRiversAppBuilder> startRapidApplication(
    appBuilder: (env: Miljoevariabler) -> A,
    createRiverConsumer: (rc: RapidsConnection, service: A) -> Unit
) {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    val ab = appBuilder(Miljoevariabler(env))
    val rapidsConnection = RapidApplication.create(env)
    createRiverConsumer(rapidsConnection, ab)
    rapidsConnection.start()
}