package no.nav.etterlatte

import no.nav.etterlatte.migrering.AvbrytBehandlingHvisMigreringFeilaRiver
import no.nav.etterlatte.regulering.FinnSakerTilReguleringRiver
import no.nav.etterlatte.regulering.OmregningsHendelserBehandlingRiver
import no.nav.etterlatte.regulering.ReguleringFeiletRiver
import no.nav.etterlatte.regulering.ReguleringsforespoerselRiver
import no.nav.etterlatte.regulering.VedtakAttestertRiver
import no.nav.etterlatte.regulering.YtelseIkkeLoependeRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR

fun main() {
    initRogR("oppdater-behandling") { rapidsConnection, rapidEnv -> settOppRivers(rapidsConnection, AppBuilder(rapidEnv)) }
}

private fun settOppRivers(
    rapidsConnection: RapidsConnection,
    appBuilder: AppBuilder,
) {
    val behandlingservice = appBuilder.behandlingService
    val tidshendelseService = appBuilder.tidshendelserService
    val featureToggleService = appBuilder.featureToggleService

    PdlHendelserRiver(rapidsConnection, behandlingservice)
    OmregningsHendelserBehandlingRiver(rapidsConnection, behandlingservice)
    FinnSakerTilReguleringRiver(rapidsConnection, behandlingservice)
    ReguleringsforespoerselRiver(rapidsConnection, behandlingservice, featureToggleService)
    ReguleringFeiletRiver(rapidsConnection, behandlingservice)
    VedtakAttestertRiver(rapidsConnection, behandlingservice)
    AvbrytBehandlingHvisMigreringFeilaRiver(rapidsConnection, behandlingservice)
    YtelseIkkeLoependeRiver(rapidsConnection, behandlingservice)
    OpprettBrevRiver(rapidsConnection, behandlingservice, featureToggleService)
    TidshendelseRiver(rapidsConnection, tidshendelseService)
    OppdaterDoedshendelseBrevDistribuert(rapidsConnection, behandlingservice)
    InntektsjusteringInfobrevRiver(rapidsConnection, behandlingservice)
}
