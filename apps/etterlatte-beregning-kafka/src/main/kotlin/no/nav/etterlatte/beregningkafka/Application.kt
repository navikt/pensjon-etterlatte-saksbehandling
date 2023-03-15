package no.nav.etterlatte

import no.nav.etterlatte.beregningkafka.AppBuilder
import no.nav.etterlatte.beregningkafka.OmregningHendelser
import no.nav.etterlatte.rapidsandrivers.startRapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = startRapidApplication(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder -> OmregningHendelser(rc, ab.createBeregningService()) }
)