package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoerselRiver
import no.nav.etterlatte.regulering.OpprettVedtakforespoerselRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.AldersovergangSjekkLoependeYtelseRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.AttestertVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.SamordningMottattRiver
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val vedtakKlient = AppBuilder(Miljoevariabler(rapidEnv)).lagVedtakKlient()
        LoependeYtelserforespoerselRiver(rapidsConnection, vedtakKlient)
        OpprettVedtakforespoerselRiver(rapidsConnection, vedtakKlient)
        MigreringHendelserRiver(rapidsConnection, vedtakKlient)
        LagreIverksattVedtakRiver(rapidsConnection, vedtakKlient)
        AttestertVedtakRiver(rapidsConnection, vedtakKlient)
        SamordningMottattRiver(rapidsConnection, vedtakKlient)
        AldersovergangSjekkLoependeYtelseRiver(rapidsConnection, vedtakKlient)
    }.start()
}
