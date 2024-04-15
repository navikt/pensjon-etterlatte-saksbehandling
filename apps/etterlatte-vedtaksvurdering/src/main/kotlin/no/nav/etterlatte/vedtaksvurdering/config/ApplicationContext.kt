package no.nav.etterlatte.vedtaksvurdering.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakKlageService
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrics
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics.VedtakMetrikkerDao
import no.nav.etterlatte.vedtaksvurdering.AutomatiskBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakSamordningService
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRapidService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.SamKlientImpl
import no.nav.etterlatte.vedtaksvurdering.klienter.TrygdetidKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.vedtaksvurdering.outbox.OutboxJob
import no.nav.etterlatte.vedtaksvurdering.outbox.OutboxRepository
import no.nav.etterlatte.vedtaksvurdering.outbox.OutboxService
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )

enum class VedtaksvurderingFeatureToggle(private val key: String) : FeatureToggle {
    Foreldreloes("foreldreloes"),
    ;

    override fun key(): String = key
}

class ApplicationContext {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-vedtaksvurdering")
    }

    val env = System.getenv()
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
    val config: Config = ConfigFactory.load()
    val dataSource = DataSourceBuilder.createDataSource(env)

    private val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
        )

    val leaderElectionHttpClient: HttpClient = httpClient()
    val leaderElectionKlient = LeaderElection(env["ELECTOR_PATH"], leaderElectionHttpClient)
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())
    val samKlient =
        SamKlientImpl(
            config,
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("samordnevedtak.azure.scope"),
            ),
        )
    val trygdetidKlient = TrygdetidKlient(config, httpClient())
    val vedtaksvurderingService =
        VedtaksvurderingService(repository = VedtaksvurderingRepository.using(dataSource))
    val vedtakBehandlingService =
        VedtakBehandlingService(
            repository = VedtaksvurderingRepository.using(dataSource),
            beregningKlient = BeregningKlientImpl(config, httpClient()),
            vilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
            behandlingKlient = behandlingKlient,
            samKlient = samKlient,
            trygdetidKlient = trygdetidKlient,
            featureToggleService = featureToggleService,
        )
    val vedtaksvurderingRapidService = VedtaksvurderingRapidService(publiser = ::publiser)
    val vedtakTilbakekrevingService =
        VedtakTilbakekrevingService(
            repository = VedtaksvurderingRepository(dataSource),
            rapidService = vedtaksvurderingRapidService,
            behandlingKlient = behandlingKlient,
        )
    val vedtakKlageService =
        VedtakKlageService(
            repository = VedtaksvurderingRepository(dataSource),
            rapidService = vedtaksvurderingRapidService,
        )

    val vedtakSamordningService =
        VedtakSamordningService(
            repository = VedtaksvurderingRepository(dataSource),
        )
    val automatiskBehandlingService =
        AutomatiskBehandlingService(
            vedtakBehandlingService,
            behandlingKlient,
        )

    val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            VedtakMetrics(VedtakMetrikkerDao.using(dataSource)),
            { leaderElectionKlient.isLeader() },
            Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(10, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue("JOBB_METRIKKER_OPENING_HOURS").let { OpeningHours.of(it) },
        )
    }

    private val rapid: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
        } else {
            TestProdusent()
        }

    private fun publiser(
        key: UUID,
        melding: String,
    ) {
        rapid.publiser(key.toString(), verdi = melding)
    }

    private val vedtakshendelserProdusent: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.getValue("KAFKA_VEDTAKSHENDELSER_TOPIC"))
        } else {
            object : KafkaProdusent<String, String> {
                override fun publiser(
                    noekkel: String,
                    verdi: String,
                    headers: Map<String, ByteArray>?,
                ): Pair<Int, Long> {
                    logger.info("Publiserer melding til vedtakshendelser-topic: $verdi")
                    return 0 to 0L
                }
            }
        }

    private val outboxService =
        OutboxService(
            outboxRepository = OutboxRepository(dataSource),
            vedtaksvurderingService = vedtaksvurderingService,
            publiserEksternHendelse = { key, melding ->
                vedtakshendelserProdusent.publiser(key.toString(), verdi = melding)
            },
        )

    val outboxJob: OutboxJob by lazy {
        OutboxJob(
            outboxService = outboxService,
            erLeader = { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(1, ChronoUnit.MINUTES),
        )
    }
}
