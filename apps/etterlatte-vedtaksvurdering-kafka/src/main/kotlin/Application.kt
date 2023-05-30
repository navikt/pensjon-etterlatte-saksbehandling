package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoersel
import no.nav.etterlatte.regulering.OpprettVedtakforespoersel
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val vedtakKlient = AppBuilder(Miljoevariabler(rapidEnv)).lagVedtakKlient()
        LoependeYtelserforespoersel(rapidsConnection, vedtakKlient)
        OpprettVedtakforespoersel(rapidsConnection, vedtakKlient)
        MigreringHendelser(rapidsConnection, vedtakKlient)
    }.start()
}