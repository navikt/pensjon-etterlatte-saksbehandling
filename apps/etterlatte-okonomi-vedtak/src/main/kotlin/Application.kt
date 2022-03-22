package no.nav.etterlatte

import no.nav.etterlatte.vedtaksoversetter.OppdragMapper
import no.nav.etterlatte.vedtaksoversetter.Vedtaksoversetter
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(Vedtaksoversetter::class.java)

    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }

    val srvUsername = env["srvuser"]
    val srvPassword = env["srvpwd"]
    logger.info("Serviceuser: $srvUsername")

    RapidApplication.create(env)
        .also {
            Vedtaksoversetter(
                rapidsConnection = it,
                oppdragMapper = OppdragMapper,
            )
        }.start()
}

