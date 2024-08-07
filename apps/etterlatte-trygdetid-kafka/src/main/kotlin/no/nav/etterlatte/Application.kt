package no.nav.etterlatte

import no.nav.etterlatte.trygdetid.kafka.AppBuilder
import no.nav.etterlatte.trygdetid.kafka.KopierTrygdetidRiver
import rapidsandrivers.initRogR

fun main() =
    initRogR("trygdetid-kafka") { rapidsConnection, rapidEnv ->
        KopierTrygdetidRiver(rapidsConnection, AppBuilder(rapidEnv).createTrygdetidService())
    }
