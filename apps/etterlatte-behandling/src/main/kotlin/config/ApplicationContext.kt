package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.EnhetServiceImpl
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.GyldighetsproevingServiceImpl
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.klage.KlageServiceImpl
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientObo
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingServiceImpl
import no.nav.etterlatte.common.klienter.PdlKlientImpl
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.databaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseJob
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.metrics.OppgaveMetrics
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgave.GosysOppgaveKlient
import no.nav.etterlatte.oppgave.GosysOppgaveKlientImpl
import no.nav.etterlatte.oppgave.GosysOppgaveServiceImpl
import no.nav.etterlatte.oppgaveny.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgaveny.OppgaveDaoNyImpl
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import java.time.Duration
import java.time.temporal.ChronoUnit

private fun pdlHttpClient(config: Config) = httpClientClientCredentials(
    azureAppClientId = config.getString("azure.app.client.id"),
    azureAppJwk = config.getString("azure.app.jwk"),
    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
    azureAppScope = config.getString("pdl.azure.scope")
)

private fun skjermingHttpClient(config: Config) = httpClientClientCredentials(
    azureAppClientId = config.getString("azure.app.client.id"),
    azureAppJwk = config.getString("azure.app.jwk"),
    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
    azureAppScope = config.getString("skjerming.azure.scope")
)

private fun grunnlagHttpClient(config: Config) = httpClientClientCredentials(
    azureAppClientId = config.getString("azure.app.client.id"),
    azureAppJwk = config.getString("azure.app.jwk"),
    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
    azureAppScope = config.getString("grunnlag.azure.scope")
)

private fun navAnsattHttpClient(config: Config) = httpClientClientCredentials(
    azureAppClientId = config.getString("azure.app.client.id"),
    azureAppJwk = config.getString("azure.app.jwk"),
    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
    azureAppScope = config.getString("navansatt.azure.scope")
)

private fun featureToggleProperties(config: Config) = FeatureToggleProperties(
    applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
    host = config.getString("funksjonsbrytere.unleash.host"),
    apiKey = config.getString("funksjonsbrytere.unleash.token")
)

class ApplicationContext(
    val env: Miljoevariabler = Miljoevariabler(System.getenv()),
    val config: Config = ConfigFactory.load(),
    val rapid: KafkaProdusent<String, String> =
        if (env["DEV"] == "true") {
            TestProdusent()
        } else {
            GcpKafkaConfig.fromEnv(env.props).standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
        },
    val featureToggleService: FeatureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config)),
    val pdlHttpClient: HttpClient = pdlHttpClient(config),
    val skjermingHttpKlient: HttpClient = skjermingHttpClient(config),
    val grunnlagHttpClient: HttpClient = grunnlagHttpClient(config),
    val navAnsattKlient: NavAnsattKlient = NavAnsattKlientImpl(
        navAnsattHttpClient(config),
        env.getValue("NAVANSATT_URL")
    ).also {
        it.asyncPing()
    },
    val norg2Klient: Norg2Klient = Norg2KlientImpl(httpClient(), env.getValue("NORG2_URL")),
    val leaderElectionHttpClient: HttpClient = httpClient(),
    val grunnlagKlientObo: GrunnlagKlient = GrunnlagKlientObo(config, httpClient()),
    val gosysOppgaveKlient: GosysOppgaveKlient = GosysOppgaveKlientImpl(config, httpClient())
) {
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.values().associateWith { env.requireEnvValue(it.envKey) }
    val sporingslogg = Sporingslogg()
    val dataSource = DataSourceBuilder.createDataSource(env.props)

    // Dao
    val hendelseDao = HendelseDao { databaseContext().activeTx() }
    val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao { databaseContext().activeTx() }
    val revurderingDao = RevurderingDao { databaseContext().activeTx() }
    val behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao) { databaseContext().activeTx() }
    val oppgaveDaoNy = OppgaveDaoNyImpl { databaseContext().activeTx() }
    val oppgaveDaoEndringer = OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy) { databaseContext().activeTx() }
    val sakDao = SakDao { databaseContext().activeTx() }
    val grunnlagsendringshendelseDao = GrunnlagsendringshendelseDao { databaseContext().activeTx() }
    val institusjonsoppholdDao = InstitusjonsoppholdDao { databaseContext().activeTx() }
    val metrikkerDao = OppgaveMetrikkerDao(dataSource)
    val klageDao = KlageDaoImpl { databaseContext().activeTx() }

    // Klient
    val pdlKlient = PdlKlientImpl(config, pdlHttpClient)
    val skjermingKlient = SkjermingKlient(skjermingHttpKlient, env.getValue("SKJERMING_URL"))
    val grunnlagKlient = GrunnlagKlientImpl(config, grunnlagHttpClient)
    val leaderElectionKlient = LeaderElection(env.getValue("ELECTOR_PATH"), leaderElectionHttpClient)

    val behandlingsHendelser = BehandlingsHendelserKafkaProducerImpl(rapid)

    // Metrikker
    val oppgaveMetrikker = OppgaveMetrics(metrikkerDao)

    // Service
    val oppgaveServiceNy = OppgaveServiceNy(oppgaveDaoEndringer, sakDao, featureToggleService)
    val gosysOppgaveService = GosysOppgaveServiceImpl(gosysOppgaveKlient, pdlKlient, featureToggleService)
    val behandlingService = BehandlingServiceImpl(
        behandlingDao = behandlingDao,
        behandlingHendelser = behandlingsHendelser,
        hendelseDao = hendelseDao,
        grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
        grunnlagKlient = grunnlagKlientObo,
        sporingslogg = sporingslogg,
        featureToggleService = featureToggleService,
        kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
        oppgaveServiceNy = oppgaveServiceNy
    )

    val kommerBarnetTilGodeService =
        KommerBarnetTilGodeService(kommerBarnetTilGodeDao, behandlingDao)
    val grunnlagsService = GrunnlagService(grunnlagKlient = grunnlagKlient)
    val revurderingService =
        RevurderingServiceImpl(
            oppgaveService = oppgaveServiceNy,
            grunnlagService = grunnlagsService,
            behandlingHendelser = behandlingsHendelser,
            featureToggleService = featureToggleService,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            revurderingDao = revurderingDao,
            behandlingService = behandlingService
        )

    val gyldighetsproevingService =
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
            featureToggleService = featureToggleService
        )

    val manueltOpphoerService =
        RealManueltOpphoerService(
            oppgaveService = oppgaveServiceNy,
            behandlingDao = behandlingDao,
            behandlingHendelser = behandlingsHendelser,
            hendelseDao = hendelseDao,
            grunnlagService = grunnlagsService,
            featureToggleService = featureToggleService
        )

    val omregningService =
        OmregningService(
            behandlingService = behandlingService,
            grunnlagService = grunnlagsService,
            revurderingService = revurderingService
        )

    val tilgangService = TilgangServiceImpl(SakTilgangDao(dataSource))
    val sakService = RealSakService(
        sakDao,
        pdlKlient,
        norg2Klient,
        featureToggleService,
        tilgangService,
        skjermingKlient
    )
    val enhetService = EnhetServiceImpl(navAnsattKlient)
    val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveServiceNy,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdlKlient = pdlKlient,
            grunnlagKlient = grunnlagKlient,
            tilgangService = tilgangService,
            sakService = sakService
        )

    val behandlingsStatusService =
        BehandlingStatusServiceImpl(behandlingDao, behandlingService, grunnlagsendringshendelseService)

    val behandlingFactory = BehandlingFactory(
        oppgaveService = oppgaveServiceNy,
        grunnlagService = grunnlagsService,
        revurderingService = revurderingService,
        sakDao = sakDao,
        behandlingDao = behandlingDao,
        hendelseDao = hendelseDao,
        behandlingHendelser = behandlingsHendelser,
        featureToggleService = featureToggleService
    )

    val migreringService = MigreringService(
        sakService = sakService,
        gyldighetsproevingService = gyldighetsproevingService,
        behandlingFactory = behandlingFactory,
        kommerBarnetTilGodeService = kommerBarnetTilGodeService,
        behandlingsHendelser = behandlingsHendelser,
        behandlingService = behandlingService,
        oppgaveServiceNy = oppgaveServiceNy
    )

    val klageService = KlageServiceImpl(
        klageDao = klageDao,
        sakDao = sakDao,
        hendelseDao = hendelseDao,
        oppgaveServiceNy = oppgaveServiceNy
    )

    // Job
    val grunnlagsendringshendelseJob = GrunnlagsendringshendelseJob(
        datasource = dataSource,
        grunnlagsendringshendelseService = grunnlagsendringshendelseService,
        leaderElection = leaderElectionKlient,
        initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
        periode = Duration.of(env.getValue("HENDELSE_JOB_FREKVENS").toLong(), ChronoUnit.MINUTES),
        minutterGamleHendelser = env.getValue("HENDELSE_MINUTTER_GAMLE_HENDELSER").toLong()
    )
}