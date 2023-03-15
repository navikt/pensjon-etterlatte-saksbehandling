package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.startRapidApplication
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoersel
import no.nav.etterlatte.regulering.OpprettVedtakforespoersel
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = startRapidApplication(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder ->
        LoependeYtelserforespoersel(rc, ab.lagVedtakKlient())
        OpprettVedtakforespoersel(rc, ab.lagVedtakKlient())
    }
)