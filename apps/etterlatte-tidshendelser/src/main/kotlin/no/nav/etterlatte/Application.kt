package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.tidshendelser.AppBuilder
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    val miljoevariabler = Miljoevariabler(rapidEnv)

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val appBuilder = AppBuilder(miljoevariabler)
    }.start()
}
