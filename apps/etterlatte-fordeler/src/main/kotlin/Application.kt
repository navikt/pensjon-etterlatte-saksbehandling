package no.nav.etterlatte

import no.nav.etterlatte.fordeler.FordelerRiver
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication

val sikkerLogg = sikkerlogger()

fun main() {
    val rapidEnv = getRapidEnv()

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        FordelerRiver(
            rapidsConnection = rapidsConnection,
            behandlingKlient = ab.behandlingKlient(),
        )
    }.start()
}
