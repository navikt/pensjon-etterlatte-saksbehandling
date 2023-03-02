package no.nav.etterlatte.utbetaling.config

import no.nav.etterlatte.libs.common.tidspunkt.klokke
import no.nav.etterlatte.libs.common.tidspunkt.nowNorskTid
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.jdbcUrl
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.common.Oppgavetrigger
import no.nav.etterlatte.utbetaling.common.april
import no.nav.etterlatte.utbetaling.common.august
import no.nav.etterlatte.utbetaling.common.februar
import no.nav.etterlatte.utbetaling.common.januar
import no.nav.etterlatte.utbetaling.common.juli
import no.nav.etterlatte.utbetaling.common.juni
import no.nav.etterlatte.utbetaling.common.mai
import no.nav.etterlatte.utbetaling.common.mars
import no.nav.etterlatte.utbetaling.common.next
import no.nav.etterlatte.utbetaling.common.november
import no.nav.etterlatte.utbetaling.common.oktober
import no.nav.etterlatte.utbetaling.common.september
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
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ApplicationContext(
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv()),
    val rapidsConnection: RapidsConnection = RapidApplication.create(System.getenv().withConsumerGroupId())
) {
    val clock = klokke()

    val dataSource = DataSourceBuilder.createDataSource(
        jdbcUrl = jdbcUrl(
            host = properties.dbHost,
            port = properties.dbPort,
            databaseName = properties.dbName
        ),
        username = properties.dbUsername,
        password = properties.dbPassword
    )

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
            starttidspunkt = nowNorskTid().next(LocalTime.of(3, 0, 0)),
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
        kjoereplanKonsistensavstemming(),
        leaderElection,
        initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
        periode = Duration.of(4, ChronoUnit.HOURS),
        clock = clock
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
}

/**
 * Kjøreplan får vi en gang i året fra økonomi og brukes for konsistensavstemming
 * av løpende/aktive utbetalinger
 */
private fun kjoereplanKonsistensavstemming() = setOf(
    5.januar(2023),
    30.januar(2023),
    27.februar(2023),
    28.mars(2023),
    25.april(2023),
    30.mai(2023),
    29.juni(2023),
    28.juli(2023),
    30.august(2023),
    29.september(2023),
    30.oktober(2023),
    22.november(2023)
)

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }