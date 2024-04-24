package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.InnsendtSoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))

        InnsendtSoeknadRiver(
            rapidsConnection,
            ab.createBehandlingClient(),
        )
    }.start()
}
