package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.barnepensjon.FordeltSoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.barnepensjon.GyldigSoeknadService
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also {
                    FordeltSoeknadRiver(
                        it,
                        GyldigSoeknadService(ab.createPdlClient()),
                        ab.createBehandlingClient()
                    )
                    InnsendtSoeknadRiver(
                        it,
                        ab.createBehandlingClient()
                    )
                }.start()
        }
    }
}