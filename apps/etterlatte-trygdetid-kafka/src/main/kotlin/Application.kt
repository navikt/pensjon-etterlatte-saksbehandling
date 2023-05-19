package no.nav.etterlatte

import BeregnTrygdetid
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.trygdetid.kafka.AppBuilder
import no.nav.etterlatte.trygdetid.kafka.MigreringHendelser
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        BeregnTrygdetid(rapidsConnection, AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService())
        MigreringHendelser(rapidsConnection, AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService())
    }.start()
}