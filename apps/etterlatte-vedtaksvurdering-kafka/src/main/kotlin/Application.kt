package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.samordning.TilSamordningRiver
import no.nav.etterlatte.regulering.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoerselRiver
import no.nav.etterlatte.regulering.OpprettVedtakforespoerselRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.TidshendelseRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.AttestertVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.SamordningMottattRiver
import rapidsandrivers.initRogR

fun main() {
    initRogR("vedtaksvurdering-kafka") { rapidsConnection, rapidEnv ->
        val appBuilder = AppBuilder(Miljoevariabler(rapidEnv))
        val vedtakKlient = appBuilder.lagVedtakKlient()
        LoependeYtelserforespoerselRiver(rapidsConnection, vedtakKlient)
        OpprettVedtakforespoerselRiver(rapidsConnection, vedtakKlient, appBuilder.lagFeatureToggleService())
        LagreIverksattVedtakRiver(rapidsConnection, vedtakKlient)
        AttestertVedtakRiver(rapidsConnection, vedtakKlient)
        SamordningMottattRiver(rapidsConnection, vedtakKlient)
        TilSamordningRiver(rapidsConnection, vedtakKlient)
        TidshendelseRiver(rapidsConnection, vedtakKlient)
    }
}
