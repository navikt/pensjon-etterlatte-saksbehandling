package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoersel
import no.nav.etterlatte.regulering.OpprettVedtakforespoersel
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also {
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        LoependeYtelserforespoersel(it, ab.lagVedtakKlient())
        OpprettVedtakforespoersel(it, ab.lagVedtakKlient())
    }.start()
}