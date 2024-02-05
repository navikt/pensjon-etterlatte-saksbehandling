package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.tidshendelser.AppBuilder
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    val miljoevariabler = Miljoevariabler(rapidEnv)

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val appBuilder = AppBuilder(miljoevariabler)
    }.start()
}
