package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    RapidApplication.create(getRapidEnv())
        .start()
        .also { StartRegulering.start() }
}