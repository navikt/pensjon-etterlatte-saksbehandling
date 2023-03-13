package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.barnepensjon.FordeltSoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.barnepensjon.GyldigSoeknadService
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.etterlatte.rapidsandrivers.init
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = init(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder ->
        FordeltSoeknadRiver(
            rc,
            GyldigSoeknadService(ab.createPdlClient()),
            ab.createBehandlingClient()
        )
        InnsendtSoeknadRiver(
            rc,
            ab.createBehandlingClient()
        )
    }
)