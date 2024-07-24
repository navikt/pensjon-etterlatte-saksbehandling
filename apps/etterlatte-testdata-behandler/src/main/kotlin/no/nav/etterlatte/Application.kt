package no.nav.etterlatte

import no.nav.etterlatte.testdata.AppBuilder
import no.nav.etterlatte.testdata.AutomatiskBehandlingRiver
import rapidsandrivers.initRogR

fun main() =
    initRogR("testdata-behandler") { rapidsConnection, _ -> AutomatiskBehandlingRiver(rapidsConnection, AppBuilder().lagBehandler()) }
