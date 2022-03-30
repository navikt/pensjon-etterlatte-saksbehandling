package no.nav.etterlatte

import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.config.JmsConnectionFactoryBuilder
import no.nav.etterlatte.oppdrag.KvitteringMottaker
import no.nav.etterlatte.oppdrag.OppdragMapper
import no.nav.etterlatte.oppdrag.OppdragSender
import no.nav.etterlatte.oppdrag.OppdragService
import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import no.nav.etterlatte.oppdrag.VedtakMottaker
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection


fun main() {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", this.required("NAIS_APP_NAME").replace("-", ""))
    }

    val dataSource = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = env.required("DB_HOST"),
            port = env.required("DB_PORT"),
            databaseName = env.required("DB_DATABASE")
        ),
        username = env.required("DB_USERNAME"),
        password = env.required("DB_PASSWORD"),
    ).apply { migrate() }.dataSource()

    val jmsConnectionFactoryBuilder = JmsConnectionFactoryBuilder(
        hostname = env.required("OPPDRAG_MQ_HOSTNAME"),
        port = env.required("OPPDRAG_MQ_PORT").toInt(),
        queueManager = env.required("OPPDRAG_MQ_MANAGER"),
        channel =  env.required("OPPDRAG_MQ_CHANNEL"),
        username = env.required("srvuser"),
        password = env.required("srvpwd")
    )

    val jmsConnection = jmsConnectionFactoryBuilder.connection()

    val oppdragSender = OppdragSender(
        jmsConnection = jmsConnection,
        queue = env.required("OPPDRAG_SEND_MQ_NAME"),
        replyQueue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    val utbetalingsoppdragDao = UtbetalingsoppdragDao(dataSource)

    val oppdragService = OppdragService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    RapidApplication.create(env)
        .apply {
            KvitteringMottaker(
                rapidsConnection = this,
                utbetalingsoppdragDao = utbetalingsoppdragDao,
                jmsConnection = jmsConnection,
                queue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
            )
            VedtakMottaker(
                rapidsConnection = this,
                oppdragService = oppdragService
            )

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

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }

private fun jdbcUrl(host: String, port: String, databaseName: String) =
    "jdbc:postgresql://${host}:$port/$databaseName"