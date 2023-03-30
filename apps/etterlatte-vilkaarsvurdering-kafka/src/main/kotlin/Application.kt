package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.vilkaarsvurdering.AppBuilder
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurder
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        Vilkaarsvurder(rapidsConnection, AppBuilder(Miljoevariabler(rapidEnv)).lagVilkaarsvurderingKlient())
    }.start()
}