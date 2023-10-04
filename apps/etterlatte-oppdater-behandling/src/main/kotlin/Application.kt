package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.migrering.AvbrytBehandlingHvisMigreringFeila
import no.nav.etterlatte.migrering.MigrerEnEnkeltSak
import no.nav.etterlatte.regulering.OmregningsHendelser
import no.nav.etterlatte.regulering.ReguleringFeilet
import no.nav.etterlatte.regulering.Reguleringsforespoersel
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val behandlingservice = AppBuilder(Miljoevariabler(rapidEnv)).createBehandlingService()
        PdlHendelser(rapidsConnection, behandlingservice)
        OmregningsHendelser(rapidsConnection, behandlingservice)
        Reguleringsforespoersel(rapidsConnection, behandlingservice)
        MigrerEnEnkeltSak(rapidsConnection, behandlingservice)
        ReguleringFeilet(rapidsConnection, behandlingservice)
        AvbrytBehandlingHvisMigreringFeila(rapidsConnection, behandlingservice)
    }.start()
}
