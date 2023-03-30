package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also {
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        val behandlingservice = ab.createBehandlingService()
        PdlHendelser(it, behandlingservice)
        OmregningsHendelser(it, behandlingservice)
        Reguleringsforespoersel(it, behandlingservice)
    }.start()
}