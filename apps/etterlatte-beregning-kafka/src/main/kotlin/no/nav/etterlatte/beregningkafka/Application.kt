package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.MigreringHendelser
import no.nav.etterlatte.beregningkafka.OmregningHendelser
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val beregningService = AppBuilder(Miljoevariabler(rapidEnv)).createBeregningService()
        OmregningHendelser(
            rapidsConnection,
            beregningService,
            AppBuilder(Miljoevariabler(rapidEnv)).createTrygdetidService()
        )
        MigreringHendelser(rapidsConnection, beregningService)
    }.start()
}