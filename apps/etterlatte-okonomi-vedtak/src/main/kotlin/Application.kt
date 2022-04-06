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
    val jmsConnectionFactory = applicationContext.jmsConnectionFactory()
    val utbetalingsoppdragDao = applicationContext.utbetalingsoppdragDao(dataSourceBuilder.dataSource())
    val oppdragService = applicationContext.oppdragService(
        oppdragSender = applicationContext.oppdragSender(jmsConnectionFactory),
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    return applicationContext.rapidsConnection()
        .apply {
            applicationContext.kvitteringMottaker(this, utbetalingsoppdragDao, jmsConnectionFactory)
            applicationContext.vedtakMottaker(this, oppdragService)

            register(object : RapidsConnection.StatusListener {
                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    jmsConnectionFactory.stop()
                }
            })
        }
}