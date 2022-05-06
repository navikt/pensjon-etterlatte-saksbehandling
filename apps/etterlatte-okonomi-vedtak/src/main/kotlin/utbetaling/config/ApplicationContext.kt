package no.nav.etterlatte.utbetaling.config

import no.nav.etterlatte.utbetaling.common.next
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittavstemmingDao
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittsavstemmingJob
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.iverksetting.KvitteringMottaker
import no.nav.etterlatte.utbetaling.iverksetting.VedtakMottaker
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource


class ApplicationContext(
    private val env: Map<String, String>,
) {

    fun clock() = Clock.systemUTC()

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

    fun utbetalingsoppdragDao(dataSource: DataSource) = UtbetalingDao(dataSource)

    fun utbetalingService(
        oppdragSender: OppdragSender,
        utbetalingDao: UtbetalingDao,
        rapidsConnection: RapidsConnection
    ) = UtbetalingService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingDao = utbetalingDao,
        rapidsConnection = rapidsConnection,
        clock = clock()
    )

    fun kvitteringMottaker(
        utbetalingService: UtbetalingService,
        jmsConnectionFactory: JmsConnectionFactory
    ) = KvitteringMottaker(
        utbetalingService = utbetalingService,
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_KVITTERING_MQ_NAME"),
    )

    fun vedtakMottaker(rapidsConnection: RapidsConnection, utbetalingService: UtbetalingService) = VedtakMottaker(
        rapidsConnection = rapidsConnection,
        utbetalingService = utbetalingService
    )

    fun avstemmingDao(dataSource: DataSource) = GrensesnittavstemmingDao(dataSource)

    fun avstemmingSender(jmsConnectionFactory: JmsConnectionFactory) = AvstemmingsdataSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = env.required("OPPDRAG_AVSTEMMING_MQ_NAME")
    )

    fun grensesnittsavstemmingService(
        grensesnittavstemmingDao: GrensesnittavstemmingDao,
        avstemmingsdataSender: AvstemmingsdataSender,
        utbetalingDao: UtbetalingDao,
    ) = GrensesnittsavstemmingService(
        grensesnittavstemmingDao = grensesnittavstemmingDao,
        avstemmingsdataSender = avstemmingsdataSender,
        utbetalingDao = utbetalingDao,
        clock = clock()
    )

    fun leaderElection() = LeaderElection(env.required("ELECTOR_PATH"))

    fun avstemmingJob(
        grensesnittsavstemmingService: GrensesnittsavstemmingService,
        leaderElection: LeaderElection,
        starttidspunkt: Date = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).next(LocalTime.of(1, 0, 0))
    ) =
        GrensesnittsavstemmingJob(
            grensesnittsavstemmingService = grensesnittsavstemmingService,
            leaderElection = leaderElection,
            starttidspunkt = starttidspunkt,
            periode = Duration.of(1, ChronoUnit.DAYS),
        )

    private fun jdbcUrl(host: String, port: String, databaseName: String) =
        "jdbc:postgresql://${host}:$port/$databaseName"
}

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
