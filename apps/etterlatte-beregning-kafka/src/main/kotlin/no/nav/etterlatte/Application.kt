package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.OmregningHendelserBeregningRiver
import no.nav.etterlatte.beregningkafka.SjekkOmOverstyrtBeregningRiver
import rapidsandrivers.initRogR

fun main() =
    initRogR("beregning-kafka") { rapidsConnection, rapidEnv ->
        val beregningService = AppBuilder(rapidEnv).createBeregningService()
        OmregningHendelserBeregningRiver(
            rapidsConnection,
            beregningService,
        )
        SjekkOmOverstyrtBeregningRiver(
            rapidsConnection,
            beregningService,
        )
    }
