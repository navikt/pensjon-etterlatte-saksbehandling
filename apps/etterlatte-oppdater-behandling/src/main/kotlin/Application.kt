package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also {
                    val behandlingservice = ab.createBehandlingService()
                    OppdaterBehandling(it, behandlingservice)
                    PdlHendelser(it, behandlingservice)
                    it.registrerVedlikeholdsriver(behandlingservice)
                }
                .start()
        }
    }
}