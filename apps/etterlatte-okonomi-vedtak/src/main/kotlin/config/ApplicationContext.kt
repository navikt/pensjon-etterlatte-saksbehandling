package no.nav.etterlatte.config

import no.nav.etterlatte.oppdrag.KvitteringMottaker
import no.nav.etterlatte.oppdrag.OppdragMapper
import no.nav.etterlatte.oppdrag.OppdragSender
import no.nav.etterlatte.oppdrag.OppdragService
import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import no.nav.etterlatte.oppdrag.VedtakMottaker
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import javax.jms.Connection
import javax.sql.DataSource


class ApplicationContext(
    private val env: Map<String, String>,
) {
    fun rapidsConnection() = RapidApplication.create(env)

    fun dataSourceBuilder() = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = env.required("DB_HOST"),
            port = env.required("DB_PORT"),
            databaseName = env.required("DB_DATABASE")
        ),
        username = env.required("DB_USERNAME"),
        password = env.required("DB_PASSWORD"),
    )

    fun jmsConnectionFactory() = JmsConnectionFactory(
        hostname = env.required("OPPDRAG_MQ_HOSTNAME"),
        port = env.required("OPPDRAG_MQ_PORT").toInt(),
        queueManager = env.required("OPPDRAG_MQ_MANAGER"),
        channel =  env.required("OPPDRAG_MQ_CHANNEL"),
        username = env.required("srvuser"),
        password = env.required("srvpwd")
    )

    fun oppdragSender(jmsConnectionFactory: JmsConnectionFactory) = OppdragSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_SEND_MQ_NAME"),
        replyQueue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    fun utbetalingsoppdragDao(dataSource: DataSource) = UtbetalingsoppdragDao(dataSource)

    fun oppdragService(oppdragSender: OppdragSender, utbetalingsoppdragDao: UtbetalingsoppdragDao) = OppdragService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    fun kvitteringMottaker(
        rapidsConnection: RapidsConnection,
        utbetalingsoppdragDao: UtbetalingsoppdragDao,
        jmsConnectionFactory: JmsConnectionFactory
    ) = KvitteringMottaker(
        rapidsConnection = rapidsConnection,
        utbetalingsoppdragDao = utbetalingsoppdragDao,
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    fun vedtakMottaker(rapidsConnection: RapidsConnection, oppdragService: OppdragService) = VedtakMottaker(
        rapidsConnection = rapidsConnection,
        oppdragService = oppdragService
    )

    private fun jdbcUrl(host: String, port: String, databaseName: String) =
        "jdbc:postgresql://${host}:$port/$databaseName"
}

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
