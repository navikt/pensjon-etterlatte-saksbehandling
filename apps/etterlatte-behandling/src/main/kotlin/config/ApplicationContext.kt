package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.GyldighetsproevingServiceImpl
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandDao
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingDao
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.jobs.DoedsmeldingJob
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.klage.KlageHendelserServiceImpl
import no.nav.etterlatte.behandling.klage.KlageServiceImpl
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlientObo
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientObo
import no.nav.etterlatte.behandling.klienter.KlageKlientImpl
import no.nav.etterlatte.behandling.klienter.MigreringKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlientImpl
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingHendelserServiceImpl
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.PesysKlientImpl
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.databaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseJobService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.metrics.BehandlingMetrics
import no.nav.etterlatte.metrics.BehandlingMetrikkerDao
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlientImpl
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveServiceImpl
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDaoTrans
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.token.Fagsaksystem
import java.time.Duration
import java.time.temporal.ChronoUnit

private fun pdlHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("pdl.azure.scope"),
    )

private fun skjermingHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("skjerming.azure.scope"),
    )

private fun grunnlagHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("grunnlag.azure.scope"),
    )

private fun navAnsattHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("navansatt.azure.scope"),
    )

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )

private fun klageHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("klage.azure.scope"),
    )

private fun tilbakekrevingHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("tilbakekreving.azure.scope"),
    )

private fun migreringHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("migrering.outbound.scope"),
    )

private fun finnBrukerIdent(): String {
    val kontekst = Kontekst.get()
    return when (kontekst) {
        null -> Fagsaksystem.EY.navn
        else -> Kontekst.get().AppUser.name()
    }
}

internal class ApplicationContext(
    val env: Miljoevariabler = Miljoevariabler(System.getenv()),
    val config: Config = ConfigFactory.load(),
    val rapid: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env.props).standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
        } else {
            TestProdusent()
        },
    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
            brukerIdent = { finnBrukerIdent() },
        ),
    val pdlHttpClient: HttpClient = pdlHttpClient(config),
    val skjermingHttpKlient: HttpClient = skjermingHttpClient(config),
    val grunnlagHttpClient: HttpClient = grunnlagHttpClient(config),
    val navAnsattKlient: NavAnsattKlient =
        NavAnsattKlientImpl(
            navAnsattHttpClient(config),
            env.getValue("NAVANSATT_URL"),
        ).also {
            it.asyncPing()
        },
    val norg2Klient: Norg2Klient = Norg2KlientImpl(httpClient(), env.getValue("NORG2_URL")),
    val leaderElectionHttpClient: HttpClient = httpClient(),
    val grunnlagKlientObo: GrunnlagKlient = GrunnlagKlientObo(config, httpClient()),
    val gosysOppgaveKlient: GosysOppgaveKlient = GosysOppgaveKlientImpl(config, httpClient()),
    val vedtakKlient: VedtakKlient = VedtakKlientImpl(config, httpClient()),
    val brevApiHttpClient: BrevApiKlient = BrevApiKlientObo(config, httpClient(forventSuksess = true)),
    val klageHttpClient: HttpClient = klageHttpClient(config),
    val tilbakekrevingHttpClient: HttpClient = tilbakekrevingHttpClient(config),
    val migreringHttpClient: HttpClient = migreringHttpClient(config),
    val pesysKlient: PesysKlient = PesysKlientImpl(config, httpClient()),
) {
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env.props)

    // Dao
    val hendelseDao = HendelseDao { databaseContext().activeTx() }
    val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao { databaseContext().activeTx() }
    val aktivitetspliktDao = AktivitetspliktDao { databaseContext().activeTx() }
    val sjekklisteDao = SjekklisteDao { databaseContext().activeTx() }
    val revurderingDao = RevurderingDao { databaseContext().activeTx() }
    val behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao) { databaseContext().activeTx() }
    val generellbehandlingDao = GenerellBehandlingDao { databaseContext().activeTx() }
    val oppgaveDaoNy = OppgaveDaoImpl { databaseContext().activeTx() }
    val oppgaveDaoEndringer = OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy) { databaseContext().activeTx() }
    val sakDao = SakDao { databaseContext().activeTx() }
    val grunnlagsendringshendelseDao = GrunnlagsendringshendelseDao { databaseContext().activeTx() }
    val institusjonsoppholdDao = InstitusjonsoppholdDao { databaseContext().activeTx() }
    val oppgaveMetrikkerDao = OppgaveMetrikkerDao(dataSource)
    val behandlingMetrikkerDao = BehandlingMetrikkerDao(dataSource)
    val klageDao = KlageDaoImpl { databaseContext().activeTx() }
    val tilbakekrevingDao = TilbakekrevingDao { databaseContext().activeTx() }
    val behandlingInfoDao = BehandlingInfoDao { databaseContext().activeTx() }
    val bosattUtlandDao = BosattUtlandDao { databaseContext().activeTx() }
    val saksbehandlerInfoDao = SaksbehandlerInfoDao(dataSource)
    val saksbehandlerInfoDaoTrans = SaksbehandlerInfoDaoTrans { databaseContext().activeTx() }
    val doedshendelseDao = DoedshendelseDao { databaseContext().activeTx() }

    // Klient
    val pdlKlient = PdlTjenesterKlientImpl(config, pdlHttpClient)
    val skjermingKlient = SkjermingKlient(skjermingHttpKlient, env.getValue("SKJERMING_URL"))
    val grunnlagKlient = GrunnlagKlientImpl(config, grunnlagHttpClient)
    val leaderElectionKlient = LeaderElection(env.maybeEnvValue("ELECTOR_PATH"), leaderElectionHttpClient)

    val behandlingsHendelser = BehandlingsHendelserKafkaProducerImpl(rapid)
    val klageHendelser = KlageHendelserServiceImpl(rapid)
    val tilbakekreving = TilbakekrevingHendelserServiceImpl(rapid)
    val klageKlient = KlageKlientImpl(klageHttpClient, resourceUrl = env.getValue("ETTERLATTE_KLAGE_API_URL"))
    val tilbakekrevingKlient =
        TilbakekrevingKlientImpl(tilbakekrevingHttpClient, resourceUrl = env.getValue("ETTERLATTE_TILBAKEKREVING_URL"))
    val migreringKlient = MigreringKlient(migreringHttpClient, env.getValue("ETTERLATTE_MIGRERING_URL"))

    // Service
    val oppgaveService = OppgaveService(oppgaveDaoEndringer, sakDao)

    val gosysOppgaveService = GosysOppgaveServiceImpl(gosysOppgaveKlient, pdlKlient)
    val grunnlagsService = GrunnlagService(grunnlagKlient)
    val behandlingService =
        BehandlingServiceImpl(
            behandlingDao = behandlingDao,
            behandlingHendelser = behandlingsHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            hendelseDao = hendelseDao,
            grunnlagKlient = grunnlagKlientObo,
            behandlingRequestLogger = behandlingRequestLogger,
            kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagsService,
        )
    val generellBehandlingService =
        GenerellBehandlingService(
            generellbehandlingDao,
            oppgaveService,
            behandlingService,
            grunnlagKlientObo,
            hendelseDao,
        )
    val kommerBarnetTilGodeService =
        KommerBarnetTilGodeService(kommerBarnetTilGodeDao, behandlingDao)
    val aktivtetspliktService = AktivitetspliktService(aktivitetspliktDao)
    val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService)

    val klageService =
        KlageServiceImpl(
            klageDao = klageDao,
            sakDao = sakDao,
            hendelseDao = hendelseDao,
            oppgaveService = oppgaveService,
            brevApiKlient = brevApiHttpClient,
            klageKlient = klageKlient,
            klageHendelser = klageHendelser,
            vedtakKlient = vedtakKlient,
        )

    val revurderingService =
        RevurderingService(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagsService,
            behandlingHendelser = behandlingsHendelser,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            revurderingDao = revurderingDao,
            klageService = klageService,
            behandlingService = behandlingService,
        )
    val automatiskRevurderingService = AutomatiskRevurderingService(revurderingService)

    val gyldighetsproevingService =
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
        )

    val omregningService =
        OmregningService(
            behandlingService = behandlingService,
            grunnlagService = grunnlagsService,
            revurderingService = automatiskRevurderingService,
        )

    val tilgangService = TilgangServiceImpl(SakTilgangDao(dataSource))
    val enhetService = BrukerServiceImpl(pdlKlient, norg2Klient)
    val sakService =
        SakServiceImpl(
            sakDao,
            skjermingKlient,
            enhetService,
        )
    val doedshendelseService = DoedshendelseService(doedshendelseDao, pdlKlient, featureToggleService)
    val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdltjenesterKlient = pdlKlient,
            grunnlagKlient = grunnlagKlient,
            sakService = sakService,
            brukerService = enhetService,
            doedshendelseService = doedshendelseService,
        )

    val doedshendelseJobService =
        DoedshendelseJobService(doedshendelseDao, featureToggleService, grunnlagsendringshendelseService, if (isProd()) 2 else 0)

    val behandlingsStatusService =
        BehandlingStatusServiceImpl(
            behandlingDao,
            behandlingService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )

    val behandlingInfoService = BehandlingInfoService(behandlingInfoDao, behandlingService, behandlingsStatusService)

    val behandlingFactory =
        BehandlingFactory(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagsService,
            revurderingService = automatiskRevurderingService,
            gyldighetsproevingService = gyldighetsproevingService,
            sakService = sakService,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            behandlingHendelser = behandlingsHendelser,
            migreringKlient = migreringKlient,
            pdltjenesterKlient = pdlKlient,
        )

    val migreringService =
        MigreringService(
            sakService = sakService,
            gyldighetsproevingService = gyldighetsproevingService,
            behandlingFactory = behandlingFactory,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            behandlingsHendelser = behandlingsHendelser,
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
        )

    val bosattUtlandService = BosattUtlandService(bosattUtlandDao = bosattUtlandDao)

    val tilbakekrevingService =
        TilbakekrevingService(
            tilbakekrevingDao = tilbakekrevingDao,
            sakDao = sakDao,
            hendelseDao = hendelseDao,
            oppgaveService = oppgaveService,
            vedtakKlient = vedtakKlient,
            tilbakekrevingKlient = tilbakekrevingKlient,
            tilbakekrevinghendelser = tilbakekreving,
        )

    val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            BehandlingMetrics(oppgaveMetrikkerDao, behandlingMetrikkerDao),
            { leaderElectionKlient.isLeader() },
            Duration.of(10, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(5, ChronoUnit.MINUTES),
        )
    }

    val doedsmeldingerJob: DoedsmeldingJob by lazy {
        DoedsmeldingJob(
            doedshendelseJobService,
            { leaderElectionKlient.isLeader() },
            0L,
            interval = if (isProd()) Duration.of(1, ChronoUnit.HOURS) else Duration.of(1, ChronoUnit.MINUTES),
        )
    }

    fun close() {
        (dataSource as HikariDataSource).close()
    }
}
