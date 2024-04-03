package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.migrering.AvbrytBehandlingHvisMigreringFeilaRiver
import no.nav.etterlatte.migrering.MigrerEnEnkeltSakRiver
import no.nav.etterlatte.migrering.OppdaterMerknadRiver
import no.nav.etterlatte.migrering.TaAvVentRiver
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.regulering.OmregningsHendelserRiver
import no.nav.etterlatte.regulering.ReguleringFeiletRiver
import no.nav.etterlatte.regulering.ReguleringsforespoerselRiver
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    val rapidEnv = getRapidEnv()
    val appBuilder = AppBuilder(Miljoevariabler(rapidEnv))
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        settOppRivers(rapidsConnection, appBuilder)
    }.start()
}

private fun settOppRivers(
    rapidsConnection: RapidsConnection,
    appBuilder: AppBuilder,
) {
    val behandlingservice = appBuilder.behandlingService
    val tidshendelseService = appBuilder.tidshendelserService
    val featureToggleService = appBuilder.featureToggleService

    PdlHendelserRiver(rapidsConnection, behandlingservice)
    OmregningsHendelserRiver(rapidsConnection, behandlingservice)
    ReguleringsforespoerselRiver(rapidsConnection, behandlingservice, featureToggleService)
    MigrerEnEnkeltSakRiver(rapidsConnection, behandlingservice)
    ReguleringFeiletRiver(rapidsConnection, behandlingservice)
    AvbrytBehandlingHvisMigreringFeilaRiver(rapidsConnection, behandlingservice)
    OpprettBrevRiver(rapidsConnection, behandlingservice, featureToggleService)
    TidshendelseRiver(rapidsConnection, tidshendelseService)
    OppdaterDoedshendelseBrevDistribuert(rapidsConnection, behandlingservice)
    TaAvVentRiver(rapidsConnection, behandlingservice)
    OppdaterMerknadRiver(rapidsConnection, behandlingservice)
}
