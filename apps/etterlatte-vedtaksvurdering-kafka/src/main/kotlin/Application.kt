package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.no.nav.etterlatte.FattVedtakEtterVentRiver
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoerselRiver
import no.nav.etterlatte.regulering.OpprettVedtakforespoerselRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.TidshendelseRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.AttestertVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.SamordningMottattRiver
import no.nav.helse.rapids_rivers.RapidApplication

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
        TidshendelseRiver(rapidsConnection, vedtakKlient)
        FattVedtakEtterVentRiver(rapidsConnection, vedtakKlient)
    }.start()
}
