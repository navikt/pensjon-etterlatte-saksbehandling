package no.nav.etterlatte.utbetaling.config

import com.typesafe.config.ConfigFactory
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
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.utbetaling.BehandlingKlient
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.avstemming.AvstemmingDao
import no.nav.etterlatte.utbetaling.avstemming.GrensesnittsavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingJob
import no.nav.etterlatte.utbetaling.avstemming.KonsistensavstemmingService
import no.nav.etterlatte.utbetaling.avstemming.avstemmingsdata.AvstemmingsdataSender
import no.nav.etterlatte.utbetaling.common.OppgavetriggerRiver
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
import no.nav.etterlatte.utbetaling.iverksetting.KvitteringMottaker
import no.nav.etterlatte.utbetaling.iverksetting.VedtakMottakRiver
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.simulering.SimuleringOsKlient
import no.nav.etterlatte.utbetaling.simulering.SimuleringOsService
import no.nav.etterlatte.utbetaling.simulering.simuleringObjectMapper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ApplicationContext(
    val env: Map<String, String> = getRapidEnv(),
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(env),
    val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(env, configFromEnvironment(env)),
        ).build(),
    val jmsConnectionFactory: EtterlatteJmsConnectionFactory =
        JmsConnectionFactory(
            hostname = properties.mqHost,
            port = properties.mqPort,
            queueManager = properties.mqQueueManager,
            channel = properties.mqChannel,
            username = properties.serviceUserUsername,
            password = properties.serviceUserPassword,
        ),
) {
    private val clock = utcKlokke()
    private val config = ConfigFactory.load()

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

    val utbetalingService =
        UtbetalingService(
            oppdragMapper = OppdragMapper,
            oppdragSender = oppdragSender,
            utbetalingDao = utbetalingDao,
            rapidsConnection = rapidsConnection,
            clock = clock,
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

    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val vedtaksvurderingKlient = VedtaksvurderingKlient(config, httpClient())

    private val simuleringOsKlient =
        SimuleringOsKlient(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("etterlatteproxy.scope"),
            ),
            objectMapper = simuleringObjectMapper(),
        )
    val simuleringOsService = SimuleringOsService(vedtaksvurderingKlient, simuleringOsKlient)

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

    val oppgavetriggerRiver by lazy {
        OppgavetriggerRiver(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService,
            grensesnittsavstemmingService = grensesnittsavstemmingService,
        )
    }

    val kvitteringMottaker by lazy {
        KvitteringMottaker(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = properties.mqKvitteringQueue,
        )
    }

    val vedtakMottakRiver by lazy {
        VedtakMottakRiver(
            rapidsConnection = rapidsConnection,
            utbetalingService = utbetalingService,
        )
    }
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
