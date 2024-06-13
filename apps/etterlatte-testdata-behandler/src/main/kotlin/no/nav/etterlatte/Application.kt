package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.testdata.AppBuilder
import no.nav.etterlatte.testdata.AutomatiskBehandlingRiver
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication
        .create(rapidEnv)
        .also { rapidsConnection ->
            AutomatiskBehandlingRiver(rapidsConnection, AppBuilder().lagBehandler())
        }.start()
}
