package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.migrering.AvbrytBehandlingHvisMigreringFeilaRiver
import no.nav.etterlatte.migrering.MigrerEnEnkeltSakRiver
import no.nav.etterlatte.regulering.OmregningsHendelserRiver
import no.nav.etterlatte.regulering.ReguleringFeiletRiver
import no.nav.etterlatte.regulering.ReguleringsforespoerselRiver
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    val appBuilder = AppBuilder(Miljoevariabler(rapidEnv))
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val behandlingservice = appBuilder.createBehandlingService()
        PdlHendelserRiver(rapidsConnection, behandlingservice)
        OmregningsHendelserRiver(rapidsConnection, behandlingservice)
        ReguleringsforespoerselRiver(rapidsConnection, behandlingservice, appBuilder.featureToggleService)
        MigrerEnEnkeltSakRiver(rapidsConnection, behandlingservice)
        ReguleringFeiletRiver(rapidsConnection, behandlingservice)
        AvbrytBehandlingHvisMigreringFeilaRiver(rapidsConnection, behandlingservice)
        OpprettBrevRiver(rapidsConnection, behandlingservice, appBuilder.featureToggleService)
    }.start()
}
