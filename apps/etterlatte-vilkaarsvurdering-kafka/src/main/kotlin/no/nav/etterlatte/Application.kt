package no.nav.etterlatte

import no.nav.etterlatte.vilkaarsvurdering.AppBuilder
import no.nav.etterlatte.vilkaarsvurdering.TidshendelseRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRiver
import rapidsandrivers.initRogR

fun main() =
    initRogR("vilkaarsvurdering-kafka") { rapidsConnection, rapidEnv ->
        val vilkaarsvurderingService = AppBuilder(rapidEnv).lagVilkaarsvurderingKlient()
        VilkaarsvurderingRiver(rapidsConnection, vilkaarsvurderingService)
        TidshendelseRiver(rapidsConnection, vilkaarsvurderingService)
    }
