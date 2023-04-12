package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BehandlingsHendelser
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.EnhetServiceImpl
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.RealGenerellBehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.RealRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.common.klienter.PdlKlientImpl
import no.nav.etterlatte.databaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleServiceProperties
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseJob
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.OppgaveServiceImpl
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.tilgangServiceImpl
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

interface BeanFactory {
    val config: Config
    fun dataSource(): DataSource
    fun sakService(): SakService
    fun tilgangService(): TilgangService
    fun foerstegangsbehandlingService(): FoerstegangsbehandlingService
    fun revurderingService(): RevurderingService
    fun generellBehandlingService(): GenerellBehandlingService
    fun grunnlagsendringshendelseService(): GrunnlagsendringshendelseService
    fun manueltOpphoerService(): ManueltOpphoerService
    fun oppgaveService(): OppgaveService
    fun omregningService(): OmregningService
    fun sakDao(): SakDao
    fun sakDaoAdressebeskyttelse(datasource: DataSource): SakTilgangDao
    fun oppgaveDao(): OppgaveDao
    fun behandlingDao(): BehandlingDao
    fun hendelseDao(): HendelseDao
    fun grunnlagsendringshendelseDao(): GrunnlagsendringshendelseDao
    fun rapid(): KafkaProdusent<String, String>
    fun behandlingHendelser(): BehandlingsHendelser
    fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory
    fun revurderingFactory(): RevurderingFactory
    fun pdlHttpClient(): HttpClient
    fun pdlKlient(): PdlKlient
    fun leaderElection(): LeaderElection
    fun grunnlagsendringshendelseJob(): Timer
    fun grunnlagHttpClient(): HttpClient
    fun grunnlagKlient(): GrunnlagKlient
    fun grunnlagKlientClientCredentials(): no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
    fun vedtakKlient(): VedtakKlient
    fun behandlingsStatusService(): BehandlingStatusService
    fun sporingslogg(): Sporingslogg
    fun getSaksbehandlerGroupIdsByKey(): Map<String, String>
    fun featureToggleService(): FeatureToggleService
    fun norg2HttpClient(): Norg2Klient
    fun navAnsattKlient(): NavAnsattKlient
    fun enhetService(): EnhetService
}

abstract class CommonFactory : BeanFactory {
    override val config: Config = ConfigFactory.load()

    private val behandlingsHendelser: BehandlingsHendelser by lazy {
        BehandlingsHendelser(
            rapid(),
            behandlingDao(),
            dataSource()
        )
    }
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory by lazy {
        FoerstegangsbehandlingFactory(
            behandlingDao(),
            hendelseDao()
        )
    }

    private val revurderingFactory: RevurderingFactory by lazy {
        RevurderingFactory(behandlingDao(), hendelseDao())
    }

    private val oppgaveService: OppgaveService by lazy {
        OppgaveServiceImpl(oppgaveDao(), featureToggleService())
    }

    private val oppgaveDao: OppgaveDao by lazy {
        OppgaveDao { databaseContext().activeTx() }
    }

    private val featureToggleService by lazy { featureToggleService() }

    override fun behandlingHendelser(): BehandlingsHendelser {
        return behandlingsHendelser
    }

    override fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory {
        return foerstegangsbehandlingFactory
    }

    override fun revurderingFactory(): RevurderingFactory {
        return revurderingFactory
    }

    override fun sakService(): SakService =
        RealSakService(sakDao(), pdlKlient(), norg2HttpClient(), featureToggleService)

    override fun tilgangService(): TilgangService =
        tilgangServiceImpl(SakTilgangDao(dataSource()), getSaksbehandlerGroupIdsByKey())

    override fun behandlingsStatusService(): BehandlingStatusService {
        return BehandlingStatusServiceImpl(behandlingDao(), generellBehandlingService())
    }

    override fun foerstegangsbehandlingService(): FoerstegangsbehandlingService =
        RealFoerstegangsbehandlingService(
            behandlingDao(),
            foerstegangsbehandlingFactory(),
            behandlingHendelser().nyHendelse
        )

    override fun revurderingService(): RevurderingService = RealRevurderingService(
        revurderingFactory(),
        behandlingHendelser().nyHendelse,
        featureToggleService
    )

    override fun manueltOpphoerService(): ManueltOpphoerService =
        RealManueltOpphoerService(
            behandlingDao(),
            behandlingHendelser().nyHendelse,
            hendelseDao()
        )

    override fun generellBehandlingService(): GenerellBehandlingService {
        return RealGenerellBehandlingService(
            behandlingDao(),
            behandlingHendelser().nyHendelse,
            foerstegangsbehandlingFactory(),
            revurderingFactory(),
            hendelseDao(),
            manueltOpphoerService(),
            vedtakKlient(),
            grunnlagKlient(),
            sporingslogg()
        )
    }

    override fun oppgaveDao(): OppgaveDao = oppgaveDao
    override fun oppgaveService(): OppgaveService = oppgaveService
    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
    override fun sakDaoAdressebeskyttelse(datasource: DataSource): SakTilgangDao =
        SakTilgangDao(datasource)

    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
    override fun hendelseDao(): HendelseDao = HendelseDao { databaseContext().activeTx() }
    override fun grunnlagsendringshendelseDao(): GrunnlagsendringshendelseDao =
        GrunnlagsendringshendelseDao { databaseContext().activeTx() }

    override fun pdlKlient() = PdlKlientImpl(pdlHttpClient(), "http://etterlatte-pdltjenester")

    override fun grunnlagKlientClientCredentials() =
        no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl(
            grunnlagHttpClient(),
            "http://etterlatte-grunnlag"
        )

    override fun grunnlagsendringshendelseService(): GrunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            grunnlagsendringshendelseDao(),
            generellBehandlingService(),
            pdlKlient(),
            grunnlagKlientClientCredentials(),
            tilgangService()
        )

    override fun grunnlagsendringshendelseJob() = GrunnlagsendringshendelseJob(
        datasource = dataSource(),
        grunnlagsendringshendelseService = grunnlagsendringshendelseService(),
        leaderElection = leaderElection(),
        initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
        periode = Duration.of(1, ChronoUnit.MINUTES),
        minutterGamleHendelser = 1L
    ).schedule()

    override fun sporingslogg(): Sporingslogg = Sporingslogg()

    override fun omregningService(): OmregningService =
        OmregningService(behandlingService = generellBehandlingService(), revurderingFactory = revurderingFactory())
}

class EnvBasedBeanFactory(private val env: Map<String, String>) : CommonFactory() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getSaksbehandlerGroupIdsByKey(): Map<String, String> {
        val attestantClaim = env["AZUREAD_ATTESTANT_GROUPID"]!!
        val saksbehandlerClaim = env["AZUREAD_SAKSBEHANDLER_GROUPID"]!!
        val strengFortroligClaim = env["AZUREAD_STRENGT_FORTROLIG_GROUPID"]!!
        val fortroligClaim = env["AZUREAD_FORTROLIG_GROUPID"]!!
        val egenAnsattClaim = env["AZUREAD_EGEN_ANSATT_GROUPID"]!!

        return mapOf(
            "AZUREAD_ATTESTANT_GROUPID" to attestantClaim,
            "AZUREAD_SAKSBEHANDLER_GROUPID" to saksbehandlerClaim,
            "AZUREAD_STRENGT_FORTROLIG_GROUPID" to strengFortroligClaim,
            "AZUREAD_FORTROLIG_GROUPID" to fortroligClaim,
            "AZUREAD_EGEN_ANSATT_GROUPID" to egenAnsattClaim
        )
    }

    private val dataSource: DataSource by lazy { DataSourceBuilder.createDataSource(env) }
    override fun dataSource() = dataSource

    override fun rapid(): KafkaProdusent<String, String> {
        return kafkaConfig().standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
    }

    private fun kafkaConfig(): KafkaConfig = GcpKafkaConfig.fromEnv(env)

    override fun pdlHttpClient() = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("pdl.azure.scope")
    )

    override fun leaderElection() = LeaderElection(env.getValue("ELECTOR_PATH"))

    override fun grunnlagHttpClient(): HttpClient = httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("grunnlag.azure.scope")
    )

    override fun vedtakKlient(): VedtakKlient {
        return VedtakKlientImpl(config, httpClient())
    }

    override fun grunnlagKlient(): GrunnlagKlient {
        return GrunnlagKlientImpl(config, httpClient())
    }

    override fun grunnlagsendringshendelseJob(): Timer {
        logger.info(
            "Setter opp GrunnlagsendringshendelseJob. LeaderElection: ${leaderElection().isLeader()} , initialDelay: ${
                Duration.of(1, ChronoUnit.MINUTES).toMillis()
            }" +
                ", periode: ${Duration.of(env.getValue("HENDELSE_JOB_FREKVENS").toLong(), ChronoUnit.MINUTES)}" +
                ", minutterGamleHendelser: ${env.getValue("HENDELSE_MINUTTER_GAMLE_HENDELSER").toLong()} "
        )
        return GrunnlagsendringshendelseJob(
            datasource = dataSource(),
            grunnlagsendringshendelseService = grunnlagsendringshendelseService(),
            leaderElection = leaderElection(),
            initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(env.getValue("HENDELSE_JOB_FREKVENS").toLong(), ChronoUnit.MINUTES),
            minutterGamleHendelser = env.getValue("HENDELSE_MINUTTER_GAMLE_HENDELSER").toLong()
        ).schedule()
    }

    override fun featureToggleService(): FeatureToggleService {
        return FeatureToggleService.initialiser(
            mapOf(
                FeatureToggleServiceProperties.ENABLED.navn to config.getString("funksjonsbrytere.enabled"),
                FeatureToggleServiceProperties.APPLICATIONNAME.navn to config.getString(
                    "funksjonsbrytere.unleash.applicationName"
                ),
                FeatureToggleServiceProperties.URI.navn to config.getString("funksjonsbrytere.unleash.uri"),
                FeatureToggleServiceProperties.CLUSTER.navn to config.getString("funksjonsbrytere.unleash.cluster")
            )
        )
    }

    override fun norg2HttpClient(): Norg2Klient {
        return Norg2KlientImpl(httpClient(), env.getValue("NORG2_URL"))
    }

    override fun navAnsattKlient(): NavAnsattKlient {
        return NavAnsattKlientImpl(
            httpClientClientCredentials(
                azureAppClientId = config.getString("azure.app.client.id"),
                azureAppJwk = config.getString("azure.app.jwk"),
                azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                azureAppScope = config.getString("navansatt.azure.scope")
            ),
            env.getValue("NAVANSATT_URL")
        )
    }

    override fun enhetService(): EnhetService {
        return EnhetServiceImpl(navAnsattKlient())
    }
}