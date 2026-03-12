package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.arbeidOgInntekt.ArbeidOgInntektKlient
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.selftest.SelfTestService
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.config.modules.DaoModule
import no.nav.etterlatte.config.modules.HighLevelServiceModule
import no.nav.etterlatte.config.modules.HttpClientFactory
import no.nav.etterlatte.config.modules.JobModule
import no.nav.etterlatte.config.modules.KafkaModule
import no.nav.etterlatte.config.modules.KlientModule
import no.nav.etterlatte.config.modules.ServiceModule
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inntektsjustering.selvbetjening.InntektsjusteringSelvbetjeningService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.tilgangsstyring.AzureGroup

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
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
    navAnsattKlientOverride: NavAnsattKlient? = null,
    norg2KlientOverride: Norg2Klient? = null,
    leaderElectionHttpClientOverride: HttpClient? = null,
    beregningKlientOverride: BeregningKlient? = null,
    trygdetidKlientOverride: TrygdetidKlient? = null,
    gosysOppgaveKlientOverride: GosysOppgaveKlient? = null,
    vedtakKlientOverride: VedtakKlient? = null,
    brevApiKlientOverride: BrevApiKlient? = null,
    brevKlientOverride: BrevKlient? = null,
    klageHttpClientOverride: HttpClient? = null,
    tilbakekrevingKlientOverride: TilbakekrevingKlient? = null,
    pesysKlientOverride: PesysKlient? = null,
    krrKlientOverride: KrrKlient? = null,
    entraProxyKlientOverride: EntraProxyKlient? = null,
    pdlTjenesterKlientOverride: PdlTjenesterKlient? = null,
    kodeverkKlientOverride: KodeverkKlient? = null,
    skjermingKlientOverride: SkjermingKlient? = null,
    inntektskomponentKlientOverride: InntektskomponentKlient? = null,
    sigrunKlientOverride: SigrunKlient? = null,
    arbeidOgInntektKlientOverride: ArbeidOgInntektKlient? = null,
    grunnlagServiceOverride: GrunnlagService? = null,
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    private val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env)
    private val autoClosingDatabase = ConnectionAutoclosingImpl(dataSource)

    private val httpClientFactory = HttpClientFactory(config)

    private val daoModule = DaoModule(autoClosingDatabase, dataSource)

    private val kafkaModule = KafkaModule(rapid)

    private val klientModule =
        KlientModule(
            config = config,
            env = env,
            httpClientFactory = httpClientFactory,
            featureToggleService = featureToggleService,
            navAnsattKlientOverride = navAnsattKlientOverride,
            norg2KlientOverride = norg2KlientOverride,
            leaderElectionHttpClientOverride = leaderElectionHttpClientOverride,
            beregningKlientOverride = beregningKlientOverride,
            trygdetidKlientOverride = trygdetidKlientOverride,
            gosysOppgaveKlientOverride = gosysOppgaveKlientOverride,
            vedtakKlientOverride = vedtakKlientOverride,
            brevApiKlientOverride = brevApiKlientOverride,
            brevKlientOverride = brevKlientOverride,
            klageHttpClientOverride = klageHttpClientOverride,
            tilbakekrevingKlientOverride = tilbakekrevingKlientOverride,
            pesysKlientOverride = pesysKlientOverride,
            krrKlientOverride = krrKlientOverride,
            entraProxyKlientOverride = entraProxyKlientOverride,
            pdlTjenesterKlientOverride = pdlTjenesterKlientOverride,
            kodeverkKlientOverride = kodeverkKlientOverride,
            skjermingKlientOverride = skjermingKlientOverride,
            inntektskomponentKlientOverride = inntektskomponentKlientOverride,
            sigrunKlientOverride = sigrunKlientOverride,
            arbeidOgInntektKlientOverride = arbeidOgInntektKlientOverride,
        )

    private val serviceModule =
        ServiceModule(
            daoModule = daoModule,
            klientModule = klientModule,
            kafkaModule = kafkaModule,
            featureToggleService = featureToggleService,
            rapid = rapid,
            grunnlagServiceOverride = grunnlagServiceOverride,
        )

    private val highLevelServiceModule =
        HighLevelServiceModule(
            daoModule = daoModule,
            klientModule = klientModule,
            kafkaModule = kafkaModule,
            serviceModule = serviceModule,
            featureToggleService = featureToggleService,
        )

    private val jobModule =
        JobModule(
            env = env,
            dataSource = dataSource,
            daoModule = daoModule,
            klientModule = klientModule,
            kafkaModule = kafkaModule,
            serviceModule = serviceModule,
            highLevelServiceModule = highLevelServiceModule,
            featureToggleService = featureToggleService,
            rapid = rapid,
        )

    val hendelseDao get() = daoModule.hendelseDao
    val kommerBarnetTilGodeDao get() = daoModule.kommerBarnetTilGodeDao
    val aktivitetspliktDao get() = daoModule.aktivitetspliktDao
    val aktivitetspliktAktivitetsgradDao get() = daoModule.aktivitetspliktAktivitetsgradDao
    val revurderingDao get() = daoModule.revurderingDao
    val behandlingDao get() = daoModule.behandlingDao
    val sakLesDao get() = daoModule.sakLesDao
    val sakSkrivDao get() = daoModule.sakSkrivDao
    val grunnlagsendringshendelseDao get() = daoModule.grunnlagsendringshendelseDao
    val institusjonsoppholdDao get() = daoModule.institusjonsoppholdDao
    val klageDao get() = daoModule.klageDao
    val tilbakekrevingDao get() = daoModule.tilbakekrevingDao
    val behandlingInfoDao get() = daoModule.behandlingInfoDao
    val sakTilgangDao get() = daoModule.sakTilgangDao
    val opplysningDao get() = daoModule.opplysningDao

    // Klienter
    val navAnsattKlient get() = klientModule.navAnsattKlient
    val norg2Klient get() = klientModule.norg2Klient
    val leaderElectionHttpClient get() = klientModule.leaderElectionHttpClient
    val beregningKlient get() = klientModule.beregningKlient
    val trygdetidKlient get() = klientModule.trygdetidKlient
    val gosysOppgaveKlient get() = klientModule.gosysOppgaveKlient
    val vedtakKlient get() = klientModule.vedtakKlient
    val brevApiKlient get() = klientModule.brevApiKlient
    val brevKlient get() = klientModule.brevKlient
    val klageHttpClient get() = klientModule.klageHttpClient
    val tilbakekrevingKlient get() = klientModule.tilbakekrevingKlient
    val pesysKlient get() = klientModule.pesysKlient
    val krrKlient get() = klientModule.krrKlient
    val entraProxyKlient get() = klientModule.entraProxyKlient
    val pdlTjenesterKlient get() = klientModule.pdlTjenesterKlient
    val kodeverkKlient get() = klientModule.kodeverkKlient
    val skjermingKlient get() = klientModule.skjermingKlient
    val inntektskomponentKlient get() = klientModule.inntektskomponentKlient
    val sigrunKlient get() = klientModule.sigrunKlient
    val arbeidOgInntektKlient get() = klientModule.arbeidOgInntektKlient

    // Services
    val brukerService get() = serviceModule.brukerService
    val saksbehandlerService get() = serviceModule.saksbehandlerService
    val oppgaveService get() = serviceModule.oppgaveService
    val oppgaveKommentarService get() = serviceModule.oppgaveKommentarService
    val nyAldersovergangService get() = serviceModule.nyAldersovergangService
    val sakTilgang get() = serviceModule.sakTilgang
    val oppdaterTilgangService get() = serviceModule.oppdaterTilgangService
    val grunnlagService get() = serviceModule.grunnlagService
    val kodeverkService get() = serviceModule.kodeverkService
    val tilgangService get() = serviceModule.tilgangService
    val sakService get() = serviceModule.sakService
    val doedshendelseService get() = serviceModule.doedshendelseService
    val etteroppgjoerOppgaveService get() = serviceModule.etteroppgjoerOppgaveService
    val etteroppgjoerService get() = serviceModule.etteroppgjoerService
    val etteroppgjoerForbehandlingService get() = serviceModule.etteroppgjoerForbehandlingService
    val kommerBarnetTilGodeService get() = serviceModule.kommerBarnetTilGodeService
    val aktivitetspliktKopierService get() = serviceModule.aktivitetspliktKopierService
    val revurderingService get() = serviceModule.revurderingService
    val gyldighetsproevingService get() = serviceModule.gyldighetsproevingService
    val behandlingService get() = serviceModule.behandlingService
    val vilkaarsvurderingService get() = serviceModule.vilkaarsvurderingService
    val generellBehandlingService get() = serviceModule.generellBehandlingService
    val behandlingMedBrevService get() = serviceModule.behandlingMedBrevService
    val sjekklisteService get() = serviceModule.sjekklisteService
    val automatiskRevurderingService get() = serviceModule.automatiskRevurderingService
    val manuellRevurderingService get() = serviceModule.manuellRevurderingService
    val aktivitetspliktService get() = serviceModule.aktivitetspliktService
    val omregningService get() = serviceModule.omregningService
    val grunnlagsendringshendelseService get() = serviceModule.grunnlagsendringshendelseService
    val behandlingsStatusService get() = serviceModule.behandlingsStatusService

    val behandlingInfoService get() = highLevelServiceModule.behandlingInfoService
    val bosattUtlandService get() = highLevelServiceModule.bosattUtlandService
    val klageService get() = highLevelServiceModule.klageService
    val omgjoeringKlageRevurderingService get() = highLevelServiceModule.omgjoeringKlageRevurderingService
    val etteroppgjoerForbehandlingBrevService get() = highLevelServiceModule.etteroppgjoerForbehandlingBrevService
    val brevService get() = highLevelServiceModule.brevService
    val tilbakekrevingService get() = highLevelServiceModule.tilbakekrevingService
    val gosysOppgaveService get() = highLevelServiceModule.gosysOppgaveService
    val aldersovergangService get() = serviceModule.aldersovergangService
    val behandlingFactory get() = highLevelServiceModule.behandlingFactory
    val etteroppgjoerRevurderingService get() = highLevelServiceModule.etteroppgjoerRevurderingService
    val migreringService get() = highLevelServiceModule.migreringService
    val aktivitetspliktOppgaveService get() = highLevelServiceModule.aktivitetspliktOppgaveService

    val behandlingsHendelser get() = kafkaModule.behandlingsHendelser

    val lesSkatteoppgjoerHendelserJobService get() = jobModule.lesSkatteoppgjoerHendelserJobService
    val oppdaterSkatteoppgjoerIkkeMottattJobService get() = jobModule.oppdaterSkatteoppgjoerIkkeMottattJobService
    val etteroppgjoerSvarfristUtloeptJobService get() = jobModule.etteroppgjoerSvarfristUtloeptJobService
    val sjekkAdressebeskyttelseJobService get() = jobModule.sjekkAdressebeskyttelseJobService
    val sjekkAdressebeskyttelseJobDao get() = daoModule.sjekkAdressebeskyttelseJobDao
    val aarligInntektsjusteringJobbService get() = jobModule.aarligInntektsjusteringJobbService

    val inntektsjusteringSelvbetjeningService by lazy {
        InntektsjusteringSelvbetjeningService(
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
            vedtakKlient = vedtakKlient,
            rapid = rapid,
            featureToggleService = featureToggleService,
            beregningKlient = beregningKlient,
        )
    }

    val selfTestService by lazy { SelfTestService(klientModule.pingableKlienter) }

    val metrikkerJob get() = jobModule.metrikkerJob
    val uttrekkLoependeYtelseEtter67Job get() = jobModule.uttrekkLoependeYtelseEtter67Job
    val aktivitetspliktOppgaveUnntakUtloeperJob get() = jobModule.aktivitetspliktOppgaveUnntakUtloeperJob
    val doedsmeldingerJob get() = jobModule.doedsmeldingerJob
    val doedsmeldingerReminderJob get() = jobModule.doedsmeldingerReminderJob
    val saksbehandlerJob get() = jobModule.saksbehandlerJob
    val oppdaterSkatteoppgjoerIkkeMottattJob get() = jobModule.oppdaterSkatteoppgjoerIkkeMottattJob
    val etteroppgjoerSvarfristUtloeptJob get() = jobModule.etteroppgjoerSvarfristUtloeptJob
    val sjekkAdressebeskyttelseJob get() = jobModule.sjekkAdressebeskyttelseJob
    val lesSkatteoppgjoerHendelserJob get() = jobModule.lesSkatteoppgjoerHendelserJob

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
