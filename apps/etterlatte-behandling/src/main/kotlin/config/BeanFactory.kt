package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BehandlingsHendelser
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.RealGenerellBehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.regulering.ReguleringFactory
import no.nav.etterlatte.behandling.regulering.RevurderingFactory
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseJob
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.klienter.PdlKlient
import no.nav.etterlatte.klienter.PdlKlientImpl
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
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
import no.nav.etterlatte.sak.SakDaoAdressebeskyttelse
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelseImpl
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer
import javax.sql.DataSource

interface BeanFactory {
    val config: Config
    fun dataSource(): DataSource
    fun sakService(): SakService
    fun sakServiceAdressebeskyttelse(): SakServiceAdressebeskyttelse
    fun foerstegangsbehandlingService(): FoerstegangsbehandlingService
    fun generellBehandlingService(): GenerellBehandlingService
    fun grunnlagsendringshendelseService(): GrunnlagsendringshendelseService
    fun manueltOpphoerService(): ManueltOpphoerService
    fun oppgaveService(): OppgaveService
    fun omregningService(): OmregningService
    fun sakDao(): SakDao
    fun sakDaoAdressebeskyttelse(datasource: DataSource): SakDaoAdressebeskyttelse
    fun oppgaveDao(): OppgaveDao
    fun behandlingDao(): BehandlingDao
    fun hendelseDao(): HendelseDao
    fun grunnlagsendringshendelseDao(): GrunnlagsendringshendelseDao
    fun rapid(): KafkaProdusent<String, String>
    fun behandlingHendelser(): BehandlingsHendelser
    fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory
    fun revurderingFactory(): RevurderingFactory
    fun reguleringFactory(): ReguleringFactory
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
    fun getSaksbehandlerGroupIdsByKey(): Map<String, String?>
    fun norg2HttpClient(): Norg2Klient
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

    private val reguleringFactory: ReguleringFactory by lazy {
        ReguleringFactory(behandlingDao(), hendelseDao())
    }

    private val oppgaveService: OppgaveService by lazy {
        OppgaveServiceImpl(oppgaveDao())
    }

    private val oppgaveDao: OppgaveDao by lazy {
        OppgaveDao { databaseContext().activeTx() }
    }

    override fun behandlingHendelser(): BehandlingsHendelser {
        return behandlingsHendelser
    }

    override fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory {
        return foerstegangsbehandlingFactory
    }

    override fun revurderingFactory(): RevurderingFactory {
        return revurderingFactory
    }

    override fun reguleringFactory(): ReguleringFactory {
        return reguleringFactory
    }

    override fun sakService(): SakService = RealSakService(sakDao(), pdlKlient(), norg2HttpClient())

    override fun sakServiceAdressebeskyttelse(): SakServiceAdressebeskyttelse =
        SakServiceAdressebeskyttelseImpl(SakDaoAdressebeskyttelse(dataSource()))

    override fun behandlingsStatusService(): BehandlingStatusService {
        return BehandlingStatusServiceImpl(behandlingDao(), generellBehandlingService())
    }

    override fun foerstegangsbehandlingService(): FoerstegangsbehandlingService =
        RealFoerstegangsbehandlingService(
            behandlingDao(),
            foerstegangsbehandlingFactory(),
            behandlingHendelser().nyHendelse
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
            reguleringFactory(),
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
    override fun sakDaoAdressebeskyttelse(datasource: DataSource): SakDaoAdressebeskyttelse =
        SakDaoAdressebeskyttelse(datasource)

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
            sakServiceAdressebeskyttelse()
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
        OmregningService(reguleringFactory = reguleringFactory(), behandlingService = generellBehandlingService())
}

class EnvBasedBeanFactory(private val env: Map<String, String>) : CommonFactory() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getSaksbehandlerGroupIdsByKey(): Map<String, String?> {
        val attestantClaim = env["AZUREAD_ATTESTANT_GROUPID"]
        val saksbehandlerClaim = env["AZUREAD_SAKSBEHANDLER_GROUPID"]
        val fortroligClaim = env["AZUREAD_STRENGT_FORTROLIG_GROUPID"]
        return mapOf(
            "AZUREAD_ATTESTANT_GROUPID" to attestantClaim,
            "AZUREAD_SAKSBEHANDLER_GROUPID" to saksbehandlerClaim,
            "AZUREAD_STRENGT_FORTROLIG_GROUPID" to fortroligClaim
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

    override fun norg2HttpClient(): Norg2Klient {
        val client = HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson()
            }
            defaultRequest {
                header(X_CORRELATION_ID, getCorrelationId())
            }
        }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

        return Norg2KlientImpl(client, env.getValue("NORG2_URL"))
    }
}