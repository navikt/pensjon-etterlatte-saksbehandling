package no.nav.etterlatte.config

import no.nav.etterlatte.avstemming.AvstemmingDao
import no.nav.etterlatte.avstemming.AvstemmingJob
import no.nav.etterlatte.avstemming.AvstemmingSender
import no.nav.etterlatte.avstemming.AvstemmingService
import no.nav.etterlatte.common.next
import no.nav.etterlatte.oppdrag.KvitteringMottaker
import no.nav.etterlatte.oppdrag.OppdragMapper
import no.nav.etterlatte.oppdrag.OppdragSender
import no.nav.etterlatte.oppdrag.OppdragService
import no.nav.etterlatte.oppdrag.UtbetalingsoppdragDao
import no.nav.etterlatte.oppdrag.VedtakMottaker
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
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
        channel = env.required("OPPDRAG_MQ_CHANNEL"),
        username = env.required("srvuser"),
        password = env.required("srvpwd")
    )

    fun oppdragSender(jmsConnectionFactory: JmsConnectionFactory) = OppdragSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_SEND_MQ_NAME"),
        replyQueue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    fun utbetalingsoppdragDao(dataSource: DataSource) = UtbetalingsoppdragDao(dataSource)

    fun oppdragService(
        oppdragSender: OppdragSender,
        utbetalingsoppdragDao: UtbetalingsoppdragDao,
        rapidsConnection: RapidsConnection
    ) = OppdragService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingsoppdragDao = utbetalingsoppdragDao,
        rapidsConnection = rapidsConnection
    )

    fun kvitteringMottaker(
        oppdragService: OppdragService,
        jmsConnectionFactory: JmsConnectionFactory
    ) = KvitteringMottaker(
        oppdragService = oppdragService,
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    fun vedtakMottaker(rapidsConnection: RapidsConnection, oppdragService: OppdragService) = VedtakMottaker(
        rapidsConnection = rapidsConnection,
        oppdragService = oppdragService
    )

    fun avstemmingDao(dataSource: DataSource) = AvstemmingDao(dataSource)

    fun avstemmingSender(jmsConnectionFactory: JmsConnectionFactory) = AvstemmingSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_AVSTEMMING_MQ_NAME") // TODO
    )

    fun avstemmingService(
        avstemmingDao: AvstemmingDao,
        avstemmingSender: AvstemmingSender,
        utbetalingsoppdragDao: UtbetalingsoppdragDao
    ) = AvstemmingService(
        avstemmingDao = avstemmingDao,
        avstemmingSender = avstemmingSender,
        utbetalingsoppdragDao = utbetalingsoppdragDao
    )

    fun leaderElection() = LeaderElection(env.required("ELECTOR_PATH"))

    fun avstemmingJob(
        avstemmingService: AvstemmingService,
        leaderElection: LeaderElection,
        starttidspunkt: Date = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).next(LocalTime.of(1, 0, 0))
    ) =
        AvstemmingJob(
            avstemmingService = avstemmingService,
            leaderElection = leaderElection,
            starttidspunkt = starttidspunkt,
            periode = Duration.of(1, ChronoUnit.DAYS)
        )

    private fun jdbcUrl(host: String, port: String, databaseName: String) =
        "jdbc:postgresql://${host}:$port/$databaseName"
}

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
