package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.selftest.SelfTestService
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.config.modules.DaoModule
import no.nav.etterlatte.config.modules.HighLevelServiceModule
import no.nav.etterlatte.config.modules.JobModule
import no.nav.etterlatte.config.modules.KafkaModule
import no.nav.etterlatte.config.modules.KlientModule
import no.nav.etterlatte.config.modules.ServiceModule
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_VEDTAKSHENDELSER_TOPIC
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.tilgangsstyring.AzureGroup

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )

private fun finnBrukerIdent(): String = Kontekst.get()?.AppUser?.name() ?: Fagsaksystem.EY.navn

internal class ApplicationContext(
    val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    val config: Config = ConfigFactory.load(),
    rapid: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.requireEnvValue(KAFKA_RAPID_TOPIC))
        } else {
            TestProdusent()
        },
    rapidVedtak: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.requireEnvValue(KAFKA_VEDTAKSHENDELSER_TOPIC))
        } else {
            TestProdusent()
        },
    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
            brukerIdent = { finnBrukerIdent() },
        ),
    klientModuleOverride: KlientModule? = null,
    grunnlagServiceOverride: GrunnlagService? = null,
    vedtakKlientOverride: VedtakKlient? = null,
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    private val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env)
    private val autoClosingDatabase = ConnectionAutoclosingImpl(dataSource)

    private val daoModule = DaoModule(autoClosingDatabase, dataSource)

    private val kafkaModule = KafkaModule(rapid, rapidVedtak)

    private val klientModule =
        klientModuleOverride ?: KlientModule(
            config = config,
            env = env,
            featureToggleService = featureToggleService,
        )

    private val serviceModule: ServiceModule =
        ServiceModule(
            daoModule = daoModule,
            klientModule = klientModule,
            kafkaModule = kafkaModule,
            featureToggleService = featureToggleService,
            rapid = rapid,
            env = env,
            grunnlagServiceOverride = grunnlagServiceOverride,
            vedtakKlientOverride = vedtakKlientOverride,
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
            featureToggleService = featureToggleService,
            rapid = rapid,
        )

    // Dao
    val hendelseDao get() = daoModule.hendelseDao
    val kommerBarnetTilGodeDao get() = daoModule.kommerBarnetTilGodeDao
    val aktivitetspliktDao get() = daoModule.aktivitetspliktDao
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

    // Klient
    val norg2Klient get() = klientModule.norg2Klient
    val beregningKlient get() = klientModule.beregningKlient
    val brevApiKlient get() = klientModule.brevApiKlient
    val brevKlient get() = klientModule.brevKlient
    val krrKlient get() = klientModule.krrKlient
    val pdlTjenesterKlient get() = klientModule.pdlTjenesterKlient
    val skjermingKlient get() = klientModule.skjermingKlient
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
    val inntektsjusteringSelvbetjeningService get() = serviceModule.inntektsjusteringSelvbetjeningService
    val aldersovergangService get() = serviceModule.aldersovergangService

    // High-level services
    val behandlingInfoService get() = highLevelServiceModule.behandlingInfoService
    val bosattUtlandService get() = highLevelServiceModule.bosattUtlandService
    val klageService get() = highLevelServiceModule.klageService
    val omgjoeringKlageRevurderingService get() = highLevelServiceModule.omgjoeringKlageRevurderingService
    val etteroppgjoerForbehandlingBrevService get() = highLevelServiceModule.etteroppgjoerForbehandlingBrevService
    val brevService get() = highLevelServiceModule.brevService
    val tilbakekrevingService get() = highLevelServiceModule.tilbakekrevingService
    val gosysOppgaveService get() = highLevelServiceModule.gosysOppgaveService
    val behandlingFactory get() = highLevelServiceModule.behandlingFactory
    val etteroppgjoerRevurderingService get() = highLevelServiceModule.etteroppgjoerRevurderingService
    val migreringService get() = highLevelServiceModule.migreringService
    val aktivitetspliktOppgaveService get() = highLevelServiceModule.aktivitetspliktOppgaveService
    val vedtaksvurderingService get() = serviceModule.vedtaksvurderingService
    val vedtakBehandlingService get() = highLevelServiceModule.vedtakBehandlingService
    val vedtaksvurderingRapidService get() = serviceModule.vedtaksvurderingRapidService
    val vedtakKlageService get() = serviceModule.vedtakKlageService
    val vedtakEtteroppgjoerService get() = serviceModule.vedtakEtteroppgjoerService
    val vedtakTilbakekrevingService get() = serviceModule.vedtakTilbakekrevingService
    val vedtakKlient get() = serviceModule.vedtakKlient

    val behandlingsHendelser get() = kafkaModule.behandlingsHendelser

    val lesSkatteoppgjoerHendelserJobService get() = jobModule.lesSkatteoppgjoerHendelserJobService
    val aarligInntektsjusteringJobbService get() = jobModule.aarligInntektsjusteringJobbService

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
    val outboxJob get() = jobModule.outboxJob

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
