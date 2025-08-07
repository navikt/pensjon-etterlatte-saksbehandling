package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.OmregningHendelserBeregningRiver
import no.nav.etterlatte.beregningkafka.SjekkOmOverstyrtBeregningRiver
import no.nav.etterlatte.beregningkafka.SjekkOmTidligAlderpensjonRiver
import rapidsandrivers.newInitRogR

fun main() =
    newInitRogR("beregning-kafka", null) { rapidsConnection, miljoevariabler ->
        val beregningService = AppBuilder(miljoevariabler).createBeregningService()
        OmregningHendelserBeregningRiver(
            rapidsConnection,
            beregningService,
        )
        SjekkOmOverstyrtBeregningRiver(
            rapidsConnection,
            beregningService,
        )

        SjekkOmTidligAlderpensjonRiver(
            rapidsConnection,
            beregningService,
        )
    }
