package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.vilkaarsvurdering.AppBuilder
import no.nav.etterlatte.vilkaarsvurdering.TidshendelseRiver
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRiver
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication
        .create(rapidEnv)
        .also { rapidsConnection ->
            val vilkaarsvurderingService = AppBuilder(Miljoevariabler(rapidEnv)).lagVilkaarsvurderingKlient()
            VilkaarsvurderingRiver(rapidsConnection, vilkaarsvurderingService)
            TidshendelseRiver(rapidsConnection, vilkaarsvurderingService)
        }.start()
}
