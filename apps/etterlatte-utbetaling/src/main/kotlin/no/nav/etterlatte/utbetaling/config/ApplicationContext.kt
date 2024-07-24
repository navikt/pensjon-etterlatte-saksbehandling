package no.nav.etterlatte.utbetaling.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.jobs.next
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.jdbcUrl
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.mq.JmsConnectionFactory
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.utbetaling.BehandlingKlient
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.avstemming.AvstemmingDao
import no.nav.etterlatte.utbetaling.avstemming.GrensesnittsavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.avstemming.vedtak.Vedtaksverifiserer
import no.nav.etterlatte.utbetaling.common.april
import no.nav.etterlatte.utbetaling.common.august
import no.nav.etterlatte.utbetaling.common.februar
import no.nav.etterlatte.utbetaling.common.januar
import no.nav.etterlatte.utbetaling.common.juli
import no.nav.etterlatte.utbetaling.common.juni
import no.nav.etterlatte.utbetaling.common.mai
import no.nav.etterlatte.utbetaling.common.mars
import no.nav.etterlatte.utbetaling.common.november
import no.nav.etterlatte.utbetaling.common.oktober
import no.nav.etterlatte.utbetaling.common.september
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.simulering.SimuleringDao
import no.nav.etterlatte.utbetaling.simulering.SimuleringOsKlient
import no.nav.etterlatte.utbetaling.simulering.SimuleringOsService
import no.nav.etterlatte.utbetaling.simulering.simuleringObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ApplicationContext(
    val env: Map<String, String> = getRapidEnv(),
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(env),
    rapidConnection: RapidsConnection? = null,
    val jmsConnectionFactory: EtterlatteJmsConnectionFactory =
        JmsConnectionFactory(
            hostname = properties.mqHost,
            port = properties.mqPort,
            queueManager = properties.mqQueueManager,
            channel = properties.mqChannel,
            username = properties.serviceUserUsername,
            password = properties.serviceUserPassword,
        ),
    // Overridable clients
    val config: Config = ConfigFactory.load(),
    httpClient: HttpClient = httpClient(),
    val behandlingKlient: BehandlingKlient = BehandlingKlient(config, httpClient),
    val vedtaksvurderingKlient: VedtaksvurderingKlient = VedtaksvurderingKlient(config, httpClient),
    val simuleringOsKlient: SimuleringOsKlient =
        SimuleringOsKlient(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("etterlatteproxy.scope"),
            ),
            objectMapper = simuleringObjectMapper(),
        ),
) {
    private val clock = utcKlokke()

    val dataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl =
                jdbcUrl(
                    host = properties.dbHost,
                    port = properties.dbPort,
                    databaseName = properties.dbName,
                ),
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    val oppdragSender =
        OppdragSender(
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqSendQueue,
            replyQueue = properties.mqKvitteringQueue,
        )

    val utbetalingDao = UtbetalingDao(dataSource)

    val vedtaksverifiserer = Vedtaksverifiserer()

    val utbetalingService =
        UtbetalingService(
            oppdragMapper = OppdragMapper,
            oppdragSender = oppdragSender,
            utbetalingDao = utbetalingDao,
            clock = clock,
            vedtaksverifiserer = vedtaksverifiserer,
        )

    val avstemmingsdataSender =
        AvstemmingsdataSender(
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqAvstemmingQueue,
        )

    val avstemmingDao = AvstemmingDao(dataSource)

    val grensesnittsavstemmingService by lazy {
        GrensesnittsavstemmingService(
            avstemmingDao = avstemmingDao,
            avstemmingsdataSender = avstemmingsdataSender,
            utbetalingDao = utbetalingDao,
            clock = clock,
        )
    }

    internal val simuleringOsService =
        SimuleringOsService(
            utbetalingDao,
            vedtaksvurderingKlient,
            SimuleringDao(dataSource),
            simuleringOsKlient,
        )

    val leaderElection = LeaderElection(properties.leaderElectorPath)

    val grensesnittavstemmingJob =
        GrensesnittsavstemmingJob(
            grensesnittsavstemmingService = grensesnittsavstemmingService,
            leaderElection = leaderElection,
            starttidspunkt = Tidspunkt.now(norskKlokke()).next(LocalTime.of(3, 0, 0)),
            periode = Duration.of(1, ChronoUnit.DAYS),
            saktype = Saktype.BARNEPENSJON,
        )
    val grensesnittavstemmingJobOMS =
        GrensesnittsavstemmingJob(
            grensesnittsavstemmingService = grensesnittsavstemmingService,
            leaderElection = leaderElection,
            starttidspunkt = Tidspunkt.now(norskKlokke()).next(LocalTime.of(3, 0, 0)),
            periode = Duration.of(1, ChronoUnit.DAYS),
            saktype = Saktype.OMSTILLINGSSTOENAD,
        )

    val konsistensavstemmingService by lazy {
        KonsistensavstemmingService(
            utbetalingDao,
            avstemmingDao,
            avstemmingsdataSender,
        )
    }

    val konsistensavstemmingJob =
        KonsistensavstemmingJob(
            konsistensavstemmingService,
            kjoereplanKonsistensavstemming(),
            leaderElection,
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(4, ChronoUnit.HOURS),
            clock = clock,
            saktype = Saktype.BARNEPENSJON,
        )

    val konsistensavstemmingJobOMS =
        KonsistensavstemmingJob(
            konsistensavstemmingService,
            kjoereplanKonsistensavstemming(),
            leaderElection,
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(4, ChronoUnit.HOURS),
            clock = clock,
            saktype = Saktype.OMSTILLINGSSTOENAD,
        )
}

/**
 * Kjøreplan får vi en gang i året fra økonomi og brukes for konsistensavstemming
 * av løpende/aktive utbetalinger
 */
private fun kjoereplanKonsistensavstemming() =
    setOf(
        4.januar(2024),
        30.januar(2024),
        28.februar(2024),
        29.mars(2024),
        25.april(2024),
        24.mai(2024),
        28.juni(2024),
        30.juli(2024),
        30.august(2024),
        27.september(2024),
        30.oktober(2024),
        25.november(2024),
    )
