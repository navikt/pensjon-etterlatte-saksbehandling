package no.nav.etterlatte

import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.config.required
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
    val utbetalingDao = applicationContext.utbetalingDao(dataSource)

    val rapidsConnection = applicationContext.rapidsConnection()
    val utbetalingService = applicationContext.utbetalingService(
        oppdragSender = applicationContext.oppdragSender(jmsConnectionFactory),
        utbetalingDao = utbetalingDao,
        rapidsConnection = rapidsConnection
    )

    val grensesnittavstemmingService = applicationContext.grensesnittsavstemmingService(
        grensesnittavstemmingDao = applicationContext.avstemmingDao(dataSource),
        avstemmingsdataSender = applicationContext.avstemmingSender(jmsConnectionFactory),
        utbetalingDao = utbetalingDao
    )

    // Jobber
    applicationContext.grensesnittavstemmingJob(grensesnittavstemmingService, applicationContext.leaderElection())
        .schedule()

    return rapidsConnection
        .apply {
            applicationContext.kvitteringMottaker(utbetalingService, jmsConnectionFactory)
            applicationContext.vedtakMottaker(this, utbetalingService)
            applicationContext.oppgavetrigger(this, utbetalingService, grensesnittavstemmingService)

            register(object : RapidsConnection.StatusListener {
                override fun onShutdown(rapidsConnection: RapidsConnection) {
                    jmsConnectionFactory.stop()
                }
            })
        }
}