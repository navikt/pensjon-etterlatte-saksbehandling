package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        MigreringHendelser(rapidsConnection, AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService())
    }.start()
}