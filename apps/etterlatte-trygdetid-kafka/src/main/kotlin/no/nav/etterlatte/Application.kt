package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.trygdetid.kafka.AppBuilder
import no.nav.etterlatte.trygdetid.kafka.KopierTrygdetidRiver
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication
        .create(rapidEnv)
        .also { rapidsConnection ->
            KopierTrygdetidRiver(
                rapidsConnection,
                AppBuilder(
                    Miljoevariabler(rapidEnv),
                ).createTrygdetidService(),
            )
        }.start()
}
