package no.nav.etterlatte

import AppBuilder
import LesGyldigSoeknadsmelding
import model.GyldigSoeknadService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also {
                    LesGyldigSoeknadsmelding(it, GyldigSoeknadService(), ab.createPdlService(), ab.createBehandlingService())
                }.start()
        }
    }
}