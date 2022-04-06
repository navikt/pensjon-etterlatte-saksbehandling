package no.nav.etterlatte

import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.config.required
import no.nav.helse.rapids_rivers.RapidsConnection


fun main() {
    ApplicationContext(
        env = System.getenv().toMutableMap().apply {
            put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
        }
    ).also { rapidApplication(it).start() }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection {
    val dataSourceBuilder = applicationContext.dataSourceBuilder().also { it.migrate() }
    val attestasjonsDao = applicationContext.attestasjonsDao(dataSourceBuilder.dataSource())
    val attestasjonService = applicationContext.attestasjonService(attestasjonsDao)

    return applicationContext.rapidsConnection()
        .apply {
            applicationContext.vedtaksMottaker(this, attestasjonService)
        }
}

