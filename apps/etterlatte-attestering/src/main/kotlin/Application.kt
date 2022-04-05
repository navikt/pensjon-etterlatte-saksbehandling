package no.nav.etterlatte

import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.config.required


fun main() {
    bootstrap(
        ApplicationContext(
            env = System.getenv().toMutableMap().apply {
                put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
            }
        )
    )
}

fun bootstrap(applicationContext: ApplicationContext) {
    val dataSource = applicationContext.dataSourceBuilder().apply { migrate() }.dataSource()
    val attestasjonsDao = applicationContext.attestasjonsDao(dataSource)
    val attestasjonService = applicationContext.attestasjonService(attestasjonsDao)

    applicationContext.rapidsConnection()
        .apply {
            applicationContext.vedtaksMottaker(this, attestasjonService)
        }.start()
}

