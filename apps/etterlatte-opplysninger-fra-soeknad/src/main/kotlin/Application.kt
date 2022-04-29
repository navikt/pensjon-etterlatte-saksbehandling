package no.nav.etterlatte

import no.nav.etterlatte.opplysningerfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.opplysningerfrasoknad.StartUthentingFraSoeknad
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        RapidApplication.create(env)
            .also { StartUthentingFraSoeknad(it, Opplysningsuthenter() ) }
            .start()
    }
}