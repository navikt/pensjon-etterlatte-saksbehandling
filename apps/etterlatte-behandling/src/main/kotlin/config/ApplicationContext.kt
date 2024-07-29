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
import no.nav.etterlatte.behandling.GrunnlagServiceImpl
import no.nav.etterlatte.behandling.GyldighetsproevingServiceImpl
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.aktivitetsplikt.OppdaterAktivitetspliktRepo
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandDao
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingDao
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.job.SaksbehandlerJobService
import no.nav.etterlatte.behandling.jobs.DoedsmeldingJob
import no.nav.etterlatte.behandling.jobs.DoedsmeldingReminderJob
import no.nav.etterlatte.behandling.jobs.OppgaveFristGaarUtJobb
import no.nav.etterlatte.behandling.jobs.SaksbehandlerJob
import no.nav.etterlatte.behandling.jobs.SendTilStatistikkJob
import no.nav.etterlatte.behandling.klage.KlageBrevService
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.klage.KlageHendelserServiceImpl
import no.nav.etterlatte.behandling.klage.KlageServiceImpl
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.AxsysKlientImpl
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlientImpl
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
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlientImpl
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlientImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.selftest.SelfTestService
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingHendelserServiceImpl
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.behandling.vedtaksbehandling.VedtaksbehandlingDao
import no.nav.etterlatte.behandling.vedtaksbehandling.VedtaksbehandlingService
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.PesysKlientImpl
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsHendelseFilter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseJobService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelserKafkaServiceImpl
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.jobs.next
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.metrics.BehandlingMetrics
import no.nav.etterlatte.metrics.BehandlingMetrikkerDao
import no.nav.etterlatte.metrics.GjenopprettingMetrikkerDao
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveFristGaarUtJobService
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlientImpl
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveServiceImpl
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.person.krr.KrrKlientImpl
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.saksbehandler.SaksbehandlerServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import java.time.Duration
import java.time.LocalTime
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

private fun krrHttKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("krr.scope"),
    )

private fun axsysKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("axsys.scope"),
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
    val beregningsKlient: BeregningKlient = BeregningKlientImpl(config, httpClient()),
    val vilkaarsvuderingKlient: VilkaarsvurderingKlient = VilkaarsvurderingKlientImpl(config, httpClient()),
    val gosysOppgaveKlient: GosysOppgaveKlient = GosysOppgaveKlientImpl(config, httpClient()),
    val vedtakKlient: VedtakKlient = VedtakKlientImpl(config, httpClient()),
    val brevApiKlient: BrevApiKlient = BrevApiKlientObo(config, httpClient(forventSuksess = true)),
    val klageHttpClient: HttpClient = klageHttpClient(config),
    val tilbakekrevingKlient: TilbakekrevingKlient =
        TilbakekrevingKlientImpl(tilbakekrevingHttpClient(config), url = env.getValue("ETTERLATTE_TILBAKEKREVING_URL")),
    val migreringHttpClient: HttpClient = migreringHttpClient(config),
    val pesysKlient: PesysKlient = PesysKlientImpl(config, httpClient()),
    val krrKlient: KrrKlient = KrrKlientImpl(krrHttKlient(config), url = config.getString("krr.url")),
    val axsysKlient: AxsysKlient = AxsysKlientImpl(axsysKlient(config), url = config.getString("axsys.url")),
    val pdlTjenesterKlient: PdlTjenesterKlient = PdlTjenesterKlientImpl(config, pdlHttpClient(config)),
) {
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env.props)

    // Dao
    val autoClosingDatabase = ConnectionAutoclosingImpl(dataSource)

    val hendelseDao = HendelseDao(autoClosingDatabase)
    val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(autoClosingDatabase)
    val aktivitetspliktDao = AktivitetspliktDao(autoClosingDatabase)
    val aktivitetspliktAktivitetsgradDao = AktivitetspliktAktivitetsgradDao(autoClosingDatabase)
    val aktivitetspliktUnntakDao = AktivitetspliktUnntakDao(autoClosingDatabase)
    val sjekklisteDao = SjekklisteDao(autoClosingDatabase)
    val revurderingDao = RevurderingDao(autoClosingDatabase)
    val behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao, autoClosingDatabase)
    val generellbehandlingDao = GenerellBehandlingDao(autoClosingDatabase)
    val vedtaksbehandlingDao = VedtaksbehandlingDao(autoClosingDatabase)
    val oppgaveDaoNy = OppgaveDaoImpl(autoClosingDatabase)
    val oppgaveDaoEndringer = OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy, autoClosingDatabase)
    val sakDao = SakDao(autoClosingDatabase)
    val grunnlagsendringshendelseDao =
        GrunnlagsendringshendelseDao(
            autoClosingDatabase,
        )
    val institusjonsoppholdDao = InstitusjonsoppholdDao(autoClosingDatabase)
    val oppgaveMetrikkerDao = OppgaveMetrikkerDao(dataSource)
    val behandlingMetrikkerDao = BehandlingMetrikkerDao(dataSource)
    val gjenopprettingMetrikkerDao = GjenopprettingMetrikkerDao(dataSource)
    val klageDao = KlageDaoImpl(autoClosingDatabase)
    val tilbakekrevingDao = TilbakekrevingDao(autoClosingDatabase)
    val behandlingInfoDao = BehandlingInfoDao(autoClosingDatabase)
    val bosattUtlandDao = BosattUtlandDao(autoClosingDatabase)
    val saksbehandlerInfoDao = SaksbehandlerInfoDao(autoClosingDatabase)
    val doedshendelseDao = DoedshendelseDao(autoClosingDatabase)
    val omregningDao = OmregningDao(autoClosingDatabase)
    val sakTilgangDao = SakTilgangDao(dataSource)

    val oppdaterAktivitetspliktRepo = OppdaterAktivitetspliktRepo(autoClosingDatabase)

    // Klient
    val skjermingKlient = SkjermingKlient(skjermingHttpKlient, env.getValue("SKJERMING_URL"))
    val grunnlagKlient = GrunnlagKlientImpl(config, grunnlagHttpClient)
    val leaderElectionKlient = LeaderElection(env.maybeEnvValue("ELECTOR_PATH"), leaderElectionHttpClient)

    val klageKlient = KlageKlientImpl(klageHttpClient, url = env.getValue("ETTERLATTE_KLAGE_API_URL"))
    val migreringKlient = MigreringKlient(migreringHttpClient, env.getValue("ETTERLATTE_MIGRERING_URL"))
    val deodshendelserProducer = DoedshendelserKafkaServiceImpl(rapid)

    val behandlingsHendelser = BehandlingsHendelserKafkaProducerImpl(rapid)

    // Service
    val klageHendelser = KlageHendelserServiceImpl(rapid)
    val tilbakekrevingHendelserService = TilbakekrevingHendelserServiceImpl(rapid)
    val oppgaveService = OppgaveService(oppgaveDaoEndringer, sakDao, hendelseDao, behandlingsHendelser)

    val grunnlagsService = GrunnlagServiceImpl(grunnlagKlient)
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
            beregningKlient = beregningsKlient,
        )
    val generellBehandlingService =
        GenerellBehandlingService(
            generellbehandlingDao,
            oppgaveService,
            behandlingService,
            grunnlagKlientObo,
            hendelseDao,
            saksbehandlerInfoDao,
        )
    val vedtaksbehandlingService =
        VedtaksbehandlingService(
            vedtaksbehandlingDao,
        )
    val kommerBarnetTilGodeService =
        KommerBarnetTilGodeService(kommerBarnetTilGodeDao, behandlingDao)
    val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService)

    val klageBrevService = KlageBrevService(brevApiKlient)
    val klageService =
        KlageServiceImpl(
            klageDao = klageDao,
            sakDao = sakDao,
            behandlingService = behandlingService,
            hendelseDao = hendelseDao,
            oppgaveService = oppgaveService,
            klageKlient = klageKlient,
            klageHendelser = klageHendelser,
            vedtakKlient = vedtakKlient,
            featureToggleService = featureToggleService,
            klageBrevService = klageBrevService,
        )

    val aktivitetspliktKopierService = AktivitetspliktKopierService(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao)

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
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
        )
    val automatiskRevurderingService = AutomatiskRevurderingService(revurderingService)

    val aktivitetspliktService =
        AktivitetspliktService(
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao = aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao = aktivitetspliktUnntakDao,
            behandlingService = behandlingService,
            grunnlagKlient = grunnlagKlientObo,
            automatiskRevurderingService = automatiskRevurderingService,
            oppgaveService = oppgaveService,
            statistikkKafkaProducer = behandlingsHendelser,
        )

    val gyldighetsproevingService =
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
        )

    val omregningService =
        OmregningService(
            behandlingService = behandlingService,
            grunnlagService = grunnlagsService,
            revurderingService = automatiskRevurderingService,
            omregningDao = omregningDao,
        )

    val tilgangService = TilgangServiceImpl(sakTilgangDao)

    val externalServices: List<Pingable> =
        listOf(
            axsysKlient,
            navAnsattKlient,
            skjermingKlient,
            grunnlagKlient,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )
    val selfTestService = SelfTestService(externalServices)
    val enhetService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient)
    val sakService =
        SakServiceImpl(
            sakDao,
            skjermingKlient,
            enhetService,
            grunnlagsService,
            krrKlient,
            pdlTjenesterKlient,
        )
    val doedshendelseService = DoedshendelseService(doedshendelseDao, pdlTjenesterKlient, featureToggleService)

    val grunnlagsendringsHendelseFilter = GrunnlagsendringsHendelseFilter(vedtakKlient, behandlingService)
    val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdltjenesterKlient = pdlTjenesterKlient,
            grunnlagKlient = grunnlagKlient,
            sakService = sakService,
            brukerService = enhetService,
            doedshendelseService = doedshendelseService,
            grunnlagsendringsHendelseFilter = grunnlagsendringsHendelseFilter,
        )

    val doedshendelseReminderJob = DoedshendelseReminderService(featureToggleService, doedshendelseDao, behandlingService, oppgaveService)
    val doedshendelseJobService =
        DoedshendelseJobService(
            doedshendelseDao = doedshendelseDao,
            doedshendelseKontrollpunktService =
                DoedshendelseKontrollpunktService(
                    pdlTjenesterKlient = pdlTjenesterKlient,
                    grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
                    oppgaveService = oppgaveService,
                    sakService = sakService,
                    pesysKlient = pesysKlient,
                    behandlingService = behandlingService,
                ),
            featureToggleService = featureToggleService,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            dagerGamleHendelserSomSkalKjoeres = if (isProd()) 5 else 0,
            deodshendelserProducer = deodshendelserProducer,
            pdlTjenesterKlient = pdlTjenesterKlient,
            grunnlagService = grunnlagsService,
            krrKlient = krrKlient,
        )

    val behandlingsStatusService =
        BehandlingStatusServiceImpl(
            behandlingDao,
            behandlingService,
            behandlingInfoDao,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
        )

    val behandlingInfoService = BehandlingInfoService(behandlingInfoDao, behandlingService, behandlingsStatusService)

    val bosattUtlandService = BosattUtlandService(bosattUtlandDao = bosattUtlandDao)

    val tilbakekrevingService =
        TilbakekrevingService(
            tilbakekrevingDao = tilbakekrevingDao,
            sakDao = sakDao,
            hendelseDao = hendelseDao,
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
            vedtakKlient = vedtakKlient,
            brevApiKlient = brevApiKlient,
            tilbakekrevingKlient = tilbakekrevingKlient,
            tilbakekrevinghendelser = tilbakekrevingHendelserService,
        )

    val saksbehandlerJobService = SaksbehandlerJobService(saksbehandlerInfoDao, navAnsattKlient, axsysKlient)
    val oppgaveFristGaarUtJobService = OppgaveFristGaarUtJobService(oppgaveService)
    val saksbehandlerService: SaksbehandlerService = SaksbehandlerServiceImpl(saksbehandlerInfoDao, axsysKlient, navAnsattKlient)
    val gosysOppgaveService = GosysOppgaveServiceImpl(gosysOppgaveKlient, oppgaveService, saksbehandlerService)
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
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            vilkaarsvurderingKlient = vilkaarsvuderingKlient,
        )

    val migreringService =
        MigreringService(
            behandlingService = behandlingService,
        )

    // Jobs
    val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            BehandlingMetrics(oppgaveMetrikkerDao, behandlingMetrikkerDao, gjenopprettingMetrikkerDao),
            { leaderElectionKlient.isLeader() },
            Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(10, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue("JOBB_METRIKKER_OPENING_HOURS").let { OpeningHours.of(it) },
        )
    }

    val doedsmeldingerJob: DoedsmeldingJob by lazy {
        DoedsmeldingJob(
            doedshendelseJobService,
            { leaderElectionKlient.isLeader() },
            Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.HOURS) else Duration.of(20, ChronoUnit.MINUTES),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
        )
    }

    val doedsmeldingerReminderJob: DoedsmeldingReminderJob by lazy {
        DoedsmeldingReminderJob(
            doedshendelseReminderJob,
            { leaderElectionKlient.isLeader() },
            Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(1, ChronoUnit.HOURS),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
            openingHours = env.requireEnvValue("JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS").let { OpeningHours.of(it) },
        )
    }

    val saksbehandlerJob: SaksbehandlerJob by lazy {
        SaksbehandlerJob(
            saksbehandlerJobService = saksbehandlerJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(1, ChronoUnit.SECONDS).toMillis(),
            interval = Duration.of(20, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue("JOBB_SAKSBEHANDLER_OPENING_HOURS").let { OpeningHours.of(it) },
        )
    }

    val oppgaveFristGaarUtJobb: OppgaveFristGaarUtJobb by lazy {
        OppgaveFristGaarUtJobb(
            erLeader = { leaderElectionKlient.isLeader() },
            starttidspunkt = Tidspunkt.now(norskKlokke()).next(LocalTime.of(3, 0, 0)),
            periode = Duration.ofDays(1),
            oppgaveFristGaarUtJobService = oppgaveFristGaarUtJobService,
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
        )
    }

    val resendAktivitetspliktJob: SendTilStatistikkJob by lazy {
        SendTilStatistikkJob(
            aktivitetspliktService = aktivitetspliktService,
            oppdaterAktivitetspliktRepo = oppdaterAktivitetspliktRepo,
            initialDelay = Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            erLeader = { leaderElectionKlient.isLeader() },
            interval = Duration.of(5, ChronoUnit.MINUTES),
        )
    }

    fun close() {
        (dataSource as HikariDataSource).close()
    }
}
