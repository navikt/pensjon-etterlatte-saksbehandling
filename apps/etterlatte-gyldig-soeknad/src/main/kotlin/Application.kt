package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.barnepensjon.FordeltSoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.barnepensjon.GyldigSoeknadService
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        FordeltSoeknadRiver(
            rapidsConnection,
            GyldigSoeknadService(ab.createPdlClient()),
            ab.createBehandlingClient()
        )
        InnsendtSoeknadRiver(
            rapidsConnection,
            ab.createBehandlingClient()
        )
    }.start()
}