package no.nav.etterlatte

import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }

    RapidApplication.create(env)
        .also {
            Vedtaksoversetter(rapidsConnection = it, oppdragsMapper = OppdragMapper)
        }.start()
}

