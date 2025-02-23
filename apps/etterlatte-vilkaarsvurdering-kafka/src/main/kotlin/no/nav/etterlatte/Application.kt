package no.nav.etterlatte

import no.nav.etterlatte.vilkaarsvurdering.AppBuilder
import rapidsandrivers.initRogR

fun main() =
    initRogR("vilkaarsvurdering-kafka") { rapidsConnection, _ ->
        val vilkaarsvurderingService = AppBuilder().lagVilkaarsvurderingKlient()
        // Slutter å lytte på rivere
        // VilkaarsvurderingRiver(rapidsConnection, vilkaarsvurderingService)
        // TidshendelseRiver(rapidsConnection, vilkaarsvurderingService)
    }
