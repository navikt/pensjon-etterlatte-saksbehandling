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
    val dataSource = applicationContext.dataSourceBuilder().also { it.migrate() }.dataSource()
    val jmsConnectionFactory = applicationContext.jmsConnectionFactory()
    val utbetalingsoppdragDao = applicationContext.utbetalingsoppdragDao(dataSource)

    val rapidsConnection = applicationContext.rapidsConnection()
    val oppdragService = applicationContext.oppdragService(
        oppdragSender = applicationContext.oppdragSender(jmsConnectionFactory),
        utbetalingsoppdragDao = utbetalingsoppdragDao,
        rapidsConnection = rapidsConnection
    )

    val avstemmingService = applicationContext.avstemmingService(
        avstemmingDao = applicationContext.avstemmingDao(dataSource),
        avstemmingSender = applicationContext.avstemmingSender(jmsConnectionFactory),
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    // Jobber
    applicationContext.avstemmingJob(avstemmingService, applicationContext.leaderElection()).planlegg()

    return rapidsConnection
        .apply {
            applicationContext.kvitteringMottaker(oppdragService, jmsConnectionFactory)
            applicationContext.vedtakMottaker(this, oppdragService)

            register(object : RapidsConnection.StatusListener {
                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    jmsConnectionFactory.stop()
                }
            })
        }
}