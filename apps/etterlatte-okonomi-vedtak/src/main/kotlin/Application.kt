package no.nav.etterlatte

import config.ApplicationContext
import config.required
import no.nav.helse.rapids_rivers.RapidsConnection


fun main() {
    bootstrap(ApplicationContext(
        env = System.getenv().toMutableMap().apply {
            put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
        }
    ))
}

fun bootstrap(applicationContext: ApplicationContext) {
    val jmsConnection = applicationContext.jmsConnectionFactoryBuilder.connection()
    val utbetalingsoppdragDao = applicationContext.utbetalingsoppdragDao(applicationContext.dataSource)
    val oppdragService = applicationContext.oppdragService(
        oppdragSender = applicationContext.oppdragSender(jmsConnection),
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    applicationContext.rapidsConnection
        .apply {
            applicationContext.kvitteringMottaker(this, utbetalingsoppdragDao, jmsConnection)
            applicationContext.vedtakMottaker(this, oppdragService)

            register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    jmsConnection.start()
                }

                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    jmsConnection.close()
                }
            })
        }.start()
}