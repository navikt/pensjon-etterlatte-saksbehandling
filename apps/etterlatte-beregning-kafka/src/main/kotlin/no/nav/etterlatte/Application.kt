package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.OmregningHendelserBeregningRiver
import no.nav.etterlatte.beregningkafka.SjekkOmOverstyrtBeregningRiver
import no.nav.etterlatte.libs.common.Miljoevariabler
import rapidsandrivers.initRogR

fun main() =
    initRogR { rapidsConnection, rapidEnv ->
        val beregningService = AppBuilder(Miljoevariabler(rapidEnv)).createBeregningService()
        OmregningHendelserBeregningRiver(
            rapidsConnection,
            beregningService,
        )
        SjekkOmOverstyrtBeregningRiver(
            rapidsConnection,
            beregningService,
        )
    }
