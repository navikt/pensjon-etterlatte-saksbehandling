package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.init
import no.nav.etterlatte.vilkaarsvurdering.AppBuilder
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurder
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = init(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder -> Vilkaarsvurder(rc, ab.lagVedtakKlient()) }
)