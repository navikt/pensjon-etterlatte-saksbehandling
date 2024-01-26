package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.trygdetid.kafka.AppBuilder
import no.nav.etterlatte.trygdetid.kafka.MigreringTrygdetidHendelserRiver
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        MigreringTrygdetidHendelserRiver(rapidsConnection, AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService())
    }.start()
}
