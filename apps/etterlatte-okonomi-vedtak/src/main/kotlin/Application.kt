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
        utbetalingDao = utbetalingsoppdragDao,
        rapidsConnection = rapidsConnection
    )

    val avstemmingService = applicationContext.avstemmingService(
        grensesnittavstemmingDao = applicationContext.avstemmingDao(dataSource),
        avstemmingsdataSender = applicationContext.avstemmingSender(jmsConnectionFactory),
        utbetalingDao = utbetalingsoppdragDao
    )

    // Jobber
    applicationContext.avstemmingJob(avstemmingService, applicationContext.leaderElection()).schedule()

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