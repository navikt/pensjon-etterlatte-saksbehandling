package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.OmregningHendelserRiver
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val beregningService = AppBuilder(Miljoevariabler(rapidEnv)).createBeregningService()
        OmregningHendelserRiver(
            rapidsConnection,
            beregningService,
            AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService(),
        )
    }.start()
}
