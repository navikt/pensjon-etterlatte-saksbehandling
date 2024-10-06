package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.ETTERLATTE_KLAGE_API_URL
import no.nav.etterlatte.EnvKey.ETTERLATTE_MIGRERING_URL
import no.nav.etterlatte.EnvKey.ETTERLATTE_TILBAKEKREVING_URL
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.EnvKey.NAVANSATT_URL
import no.nav.etterlatte.EnvKey.NORG2_URL
import no.nav.etterlatte.EnvKey.SKJERMING_URL
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
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientImpl
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
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.ManuellRevurderingService
import no.nav.etterlatte.behandling.revurdering.OmgjoeringKlageRevurderingService
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
import no.nav.etterlatte.config.JobbKeys.JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_METRIKKER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_SAKSBEHANDLER_OPENING_HOURS
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
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.jobs.next
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkKlientImpl
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
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
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.saksbehandler.SaksbehandlerServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingKlientDao
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingKlientDaoImpl
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapper
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingRepositoryWrapperDatabase
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkarsvurderingRepositorDaoWrapperClient
import no.nav.etterlatte.vilkaarsvurdering.ektedao.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.ektedao.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.service.AldersovergangService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
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
    val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    val config: Config = ConfigFactory.load(),
    val rapid: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.requireEnvValue(KAFKA_RAPID_TOPIC))
        } else {
            TestProdusent()
        },
    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
            brukerIdent = { finnBrukerIdent() },
        ),
    val skjermingHttpKlient: HttpClient = skjermingHttpClient(config),
    val navAnsattKlient: NavAnsattKlient =
        NavAnsattKlientImpl(
            navAnsattHttpClient(config),
            env.requireEnvValue(NAVANSATT_URL),
        ).also {
            it.asyncPing()
        },
    val norg2Klient: Norg2Klient = Norg2KlientImpl(httpClient(), env.requireEnvValue(NORG2_URL)),
    val leaderElectionHttpClient: HttpClient = httpClient(),
    val grunnlagKlientImpl: GrunnlagKlient = GrunnlagKlientImpl(config, httpClient()),
    val beregningsKlient: BeregningKlient = BeregningKlientImpl(config, httpClient()),
    val gosysOppgaveKlient: GosysOppgaveKlient = GosysOppgaveKlientImpl(config, httpClient()),
    val vedtakKlient: VedtakKlient = VedtakKlientImpl(config, httpClient()),
    val brevApiKlient: BrevApiKlient = BrevApiKlientObo(config, httpClient(forventSuksess = true)),
    val klageHttpClient: HttpClient = klageHttpClient(config),
    val tilbakekrevingKlient: TilbakekrevingKlient =
        TilbakekrevingKlientImpl(
            tilbakekrevingHttpClient(config),
            url = env.requireEnvValue(ETTERLATTE_TILBAKEKREVING_URL),
        ),
    val migreringHttpClient: HttpClient = migreringHttpClient(config),
    val pesysKlient: PesysKlient = PesysKlientImpl(config, httpClient()),
    val krrKlient: KrrKlient = KrrKlientImpl(krrHttKlient(config), url = config.getString("krr.url")),
    val axsysKlient: AxsysKlient = AxsysKlientImpl(axsysKlient(config), url = config.getString("axsys.url")),
    val pdlTjenesterKlient: PdlTjenesterKlient = PdlTjenesterKlientImpl(config, pdlHttpClient(config)),
    val kodeverkKlient: KodeverkKlient = KodeverkKlientImpl(config, httpClient()),
    val vilkaarsvurderingKlientDaoImpl: VilkaarsvurderingKlientDao =
        VilkaarsvurderingKlientDaoImpl(
            config,
            httpClient(),
        ),
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env)

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
    val sakLesDao = SakLesDao(autoClosingDatabase)
    val sakendringerDao = SakendringerDao(autoClosingDatabase) { sakLesDao.hentSak(it) }
    val sakSkrivDao = SakSkrivDao(sakendringerDao)
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
    val vilkaarsvurderingDao = VilkaarsvurderingRepository(autoClosingDatabase, DelvilkaarRepository())

    val vilkaarsvurderingRepositoryWrapper: VilkaarsvurderingRepositoryWrapper =
        if (isProd()) {
            VilkarsvurderingRepositorDaoWrapperClient(vilkaarsvurderingKlientDaoImpl)
        } else {
            VilkaarsvurderingRepositoryWrapperDatabase(vilkaarsvurderingDao)
        }

    // Klient
    val skjermingKlient = SkjermingKlient(skjermingHttpKlient, env.requireEnvValue(SKJERMING_URL))
    val leaderElectionKlient = LeaderElection(env[ELECTOR_PATH], leaderElectionHttpClient)

    val klageKlient = KlageKlientImpl(klageHttpClient, url = env.requireEnvValue(ETTERLATTE_KLAGE_API_URL))
    val migreringKlient = MigreringKlient(migreringHttpClient, env.requireEnvValue(ETTERLATTE_MIGRERING_URL))
    val deodshendelserProducer = DoedshendelserKafkaServiceImpl(rapid)
    val kodeverkService = KodeverkService(kodeverkKlient)

    val behandlingsHendelser = BehandlingsHendelserKafkaProducerImpl(rapid)

    // Service
    val klageHendelser = KlageHendelserServiceImpl(rapid)
    val tilbakekrevingHendelserService = TilbakekrevingHendelserServiceImpl(rapid)
    val oppgaveService = OppgaveService(oppgaveDaoEndringer, sakLesDao, hendelseDao, behandlingsHendelser)

    val grunnlagsService = GrunnlagServiceImpl(grunnlagKlientImpl)
    val behandlingService =
        BehandlingServiceImpl(
            behandlingDao = behandlingDao,
            behandlingHendelser = behandlingsHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            hendelseDao = hendelseDao,
            grunnlagKlient = grunnlagKlientImpl,
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
            grunnlagKlientImpl,
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
            sakDao = sakLesDao,
            behandlingService = behandlingService,
            hendelseDao = hendelseDao,
            oppgaveService = oppgaveService,
            klageKlient = klageKlient,
            klageHendelser = klageHendelser,
            vedtakKlient = vedtakKlient,
            featureToggleService = featureToggleService,
            klageBrevService = klageBrevService,
        )

    val aktivitetspliktKopierService =
        AktivitetspliktKopierService(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao)

    val revurderingService =
        RevurderingService(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagsService,
            behandlingHendelser = behandlingsHendelser,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            revurderingDao = revurderingDao,
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
        )
    val automatiskRevurderingService =
        AutomatiskRevurderingService(
            revurderingService,
            behandlingService,
            grunnlagsService,
            vedtakKlient,
            beregningsKlient,
        )
    val manuellRevurderingService =
        ManuellRevurderingService(
            revurderingService = revurderingService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagsService,
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
        )
    val omgjoeringKlageRevurderingService =
        OmgjoeringKlageRevurderingService(
            revurderingService = revurderingService,
            oppgaveService = oppgaveService,
            klageService = klageService,
            behandlingDao = behandlingDao,
            grunnlagService = grunnlagsService,
        )

    val aktivitetspliktService =
        AktivitetspliktService(
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao = aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao = aktivitetspliktUnntakDao,
            behandlingService = behandlingService,
            grunnlagKlient = grunnlagKlientImpl,
            revurderingService = revurderingService,
            oppgaveService = oppgaveService,
            statistikkKafkaProducer = behandlingsHendelser,
            featureToggleService = featureToggleService,
        )

    val gyldighetsproevingService =
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
        )

    val omregningService =
        OmregningService(
            behandlingService = behandlingService,
            omregningDao = omregningDao,
        )

    val tilgangService = TilgangServiceImpl(sakTilgangDao)

    val externalServices: List<Pingable> =
        listOf(
            axsysKlient,
            navAnsattKlient,
            skjermingKlient,
            grunnlagKlientImpl,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )
    val selfTestService = SelfTestService(externalServices)
    val enhetService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient)
    val sakService =
        SakServiceImpl(
            sakSkrivDao,
            sakLesDao,
            skjermingKlient,
            enhetService,
            grunnlagsService,
            krrKlient,
            pdlTjenesterKlient,
        )
    val doedshendelseService = DoedshendelseService(doedshendelseDao, pdlTjenesterKlient)

    val grunnlagsendringsHendelseFilter = GrunnlagsendringsHendelseFilter(vedtakKlient, behandlingService)
    val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdltjenesterKlient = pdlTjenesterKlient,
            grunnlagKlient = grunnlagKlientImpl,
            sakService = sakService,
            brukerService = enhetService,
            doedshendelseService = doedshendelseService,
            grunnlagsendringsHendelseFilter = grunnlagsendringsHendelseFilter,
        )

    private val doedshendelseReminderJob =
        DoedshendelseReminderService(doedshendelseDao, behandlingService, oppgaveService)
    private val doedshendelseJobService =
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
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            dagerGamleHendelserSomSkalKjoeres = if (isProd()) 5 else 0,
            deodshendelserProducer = deodshendelserProducer,
            grunnlagService = grunnlagsService,
            pdlTjenesterKlient = pdlTjenesterKlient,
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
            sakDao = sakLesDao,
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
    val saksbehandlerService: SaksbehandlerService =
        SaksbehandlerServiceImpl(saksbehandlerInfoDao, axsysKlient, navAnsattKlient)
    val gosysOppgaveService =
        GosysOppgaveServiceImpl(
            gosysOppgaveKlient,
            oppgaveService,
            saksbehandlerService,
            saksbehandlerInfoDao,
            pdlTjenesterKlient,
        )

    val vilkaarsvurderingService =
        VilkaarsvurderingService(
            vilkaarsvurderingRepositoryWrapper,
            behandlingService,
            grunnlagKlientImpl,
            behandlingsStatusService,
        )
    val aldersovergangService = AldersovergangService(vilkaarsvurderingService)
    val behandlingFactory =
        BehandlingFactory(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagsService,
            revurderingService = revurderingService,
            gyldighetsproevingService = gyldighetsproevingService,
            sakService = sakService,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            behandlingHendelser = behandlingsHendelser,
            migreringKlient = migreringKlient,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            vilkaarsvurderingService = vilkaarsvurderingService,
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
            openingHours = env.requireEnvValue(JOBB_METRIKKER_OPENING_HOURS).let { OpeningHours.of(it) },
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
            openingHours = env.requireEnvValue(JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val saksbehandlerJob: SaksbehandlerJob by lazy {
        SaksbehandlerJob(
            saksbehandlerJobService = saksbehandlerJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(1, ChronoUnit.SECONDS).toMillis(),
            interval = Duration.of(20, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue(JOBB_SAKSBEHANDLER_OPENING_HOURS).let { OpeningHours.of(it) },
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

    fun close() {
        (dataSource as HikariDataSource).close()
    }
}

enum class JobbKeys : EnvEnum {
    JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS,
    JOBB_METRIKKER_OPENING_HOURS,
    JOBB_SAKSBEHANDLER_OPENING_HOURS,
    ;

    override fun key() = name
}
