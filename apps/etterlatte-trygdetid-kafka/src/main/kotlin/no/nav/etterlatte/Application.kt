package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).start()
}
