package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
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
    }.start()
}
