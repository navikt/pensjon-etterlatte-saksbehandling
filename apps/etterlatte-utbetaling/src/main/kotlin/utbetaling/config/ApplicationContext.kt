package no.nav.etterlatte.utbetaling.config

import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.common.Oppgavetrigger
import no.nav.etterlatte.utbetaling.common.next
import no.nav.etterlatte.utbetaling.grensesnittavstemming.AvstemmingDao
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
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ApplicationContext(
    private val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv()),
    val rapidsConnection: RapidsConnection = RapidApplication.create(System.getenv().withConsumerGroupId())
) {
    val clock = Clock.systemUTC()

    val dataSourceBuilder = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword
    )

    val dataSource = dataSourceBuilder.dataSource()

    val jmsConnectionFactory = JmsConnectionFactory(
        hostname = properties.mqHost,
        port = properties.mqPort,
        queueManager = properties.mqQueueManager,
        channel = properties.mqChannel,
        username = properties.serviceUserUsername,
        password = properties.serviceUserPassword
    )

    val oppdragSender = OppdragSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = properties.mqSendQueue,
        replyQueue = properties.mqKvitteringQueue
    )

    val utbetalingDao = UtbetalingDao(dataSource)

    val utbetalingService = UtbetalingService(
        oppdragMapper = OppdragMapper,
        oppdragSender = oppdragSender,
        utbetalingDao = utbetalingDao,
        rapidsConnection = rapidsConnection,
        clock = clock
    )

    val avstemmingsdataSender = AvstemmingsdataSender(
        jmsConnectionFactory = jmsConnectionFactory,
        queue = properties.mqAvstemmingQueue
    )

    val avstemmingDao = AvstemmingDao(dataSource)

    val grensesnittsavstemmingService by lazy {
        GrensesnittsavstemmingService(
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender,
            utbetalingDao = utbetalingDao,
            clock = clock
        )
    }

    val leaderElection = LeaderElection(properties.leaderElectorPath)

    val grensesnittavstemmingJob =
        GrensesnittsavstemmingJob(
            grensesnittsavstemmingService = grensesnittsavstemmingService,
            leaderElection = leaderElection,
            starttidspunkt = ZonedDateTime.now(norskTidssone).next(LocalTime.of(3, 0, 0)),
            periode = Duration.of(1, ChronoUnit.DAYS)
        )

    val konsistensavstemmingService by lazy {
        KonsistensavstemmingService(
            utbetalingDao,
            avstemmingDao,
            avstemmingsdataSender
        )
    }

    val konsistensavstemmingJob = KonsistensavstemmingJob(
        konsistensavstemmingService,
        leaderElection,
        initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
        periode = Duration.of(1, ChronoUnit.HOURS)

    )

    val oppgavetrigger by lazy {
        Oppgavetrigger(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService,
            grensesnittsavstemmingService = grensesnittsavstemmingService
        )
    }

    val kvitteringMottaker by lazy {
        KvitteringMottaker(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqKvitteringQueue
        )
    }

    val vedtakMottaker by lazy {
        VedtakMottaker(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService
        )
    }

    private fun jdbcUrl(host: String, port: Int, databaseName: String) =
        "jdbc:postgresql://$host:$port/$databaseName"
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }