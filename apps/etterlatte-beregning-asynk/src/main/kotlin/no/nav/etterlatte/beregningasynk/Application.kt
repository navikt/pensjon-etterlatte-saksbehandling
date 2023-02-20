package no.nav.etterlatte.beregningasynk

import no.nav.etterlatte.rapidsandrivers.init
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = init(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder -> OmberegningHendelser(rc, ab.createBeregningService()) }
)