package no.nav.etterlatte

import no.nav.etterlatte.config.AppBuilder
import no.nav.etterlatte.regulering.LoependeYtelserforespoerselRiver
import no.nav.etterlatte.regulering.OpprettVedtakforespoerselRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.rivers.TidshendelseRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.AttestertVedtakRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.SamordningMottattRiver
import no.nav.etterlatte.vedtaksvurdering.samordning.TilSamordningRiver
import rapidsandrivers.initRogR

fun main() {
    initRogR("vedtaksvurdering-kafka") { rapidsConnection, rapidEnv ->
        val appBuilder = AppBuilder(rapidEnv)
        val vedtakService = appBuilder.lagVedtakService()
        val utbetalingService = appBuilder.lagUtbetalingService()
        val brevService = appBuilder.lagBrevService()
        LoependeYtelserforespoerselRiver(rapidsConnection, vedtakService)
        OpprettVedtakforespoerselRiver(
            rapidsConnection,
            vedtakService,
            utbetalingService,
            brevService,
            appBuilder.lagFeatureToggleService(),
        )
        LagreIverksattVedtakRiver(rapidsConnection, vedtakService)
        AttestertVedtakRiver(rapidsConnection, vedtakService)
        SamordningMottattRiver(rapidsConnection, vedtakService)
        TilSamordningRiver(rapidsConnection, vedtakService)
        TidshendelseRiver(rapidsConnection, vedtakService)
    }
}
