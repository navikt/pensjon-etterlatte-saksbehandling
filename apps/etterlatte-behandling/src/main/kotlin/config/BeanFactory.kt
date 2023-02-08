package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
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
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RealRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.LeaderElection
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseJob
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.klienter.PdlKlient
import no.nav.etterlatte.grunnlagsendring.klienter.PdlKlientImpl
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

interface BeanFactory {
    fun dataSource(): DataSource
    fun sakService(): SakService
    fun foerstegangsbehandlingService(): FoerstegangsbehandlingService
    fun revurderingService(): RevurderingService
    fun generellBehandlingService(): GenerellBehandlingService
    fun grunnlagsendringshendelseService(): GrunnlagsendringshendelseService
    fun manueltOpphoerService(): ManueltOpphoerService
    fun sakDao(): SakDao
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
}

abstract class CommonFactory : BeanFactory {
    private val behandlingsHendelser: BehandlingsHendelser by lazy {
        BehandlingsHendelser(
            rapid(),
            behandlingDao(),
            dataSource(),
            sakService()
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

    override fun behandlingHendelser(): BehandlingsHendelser {
        return behandlingsHendelser
    }

    override fun foerstegangsbehandlingFactory(): FoerstegangsbehandlingFactory {
        return foerstegangsbehandlingFactory
    }

    override fun revurderingFactory(): RevurderingFactory {
        return revurderingFactory
    }

    override fun sakService(): SakService = RealSakService(sakDao())

    override fun behandlingsStatusService(): BehandlingStatusService {
        return BehandlingStatusServiceImpl(behandlingDao(), generellBehandlingService())
    }

    override fun foerstegangsbehandlingService(): FoerstegangsbehandlingService =
        RealFoerstegangsbehandlingService(
            behandlingDao(),
            foerstegangsbehandlingFactory(),
            behandlingHendelser().nyHendelse
        )

    override fun revurderingService(): RevurderingService =
        RealRevurderingService(
            behandlingDao(),
            revurderingFactory(),
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
            hendelseDao(),
            manueltOpphoerService(),
            vedtakKlient(),
            grunnlagKlient(),
            sporingslogg()
        )
    }

    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
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
            grunnlagKlientClientCredentials()
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
}

class EnvBasedBeanFactory(val env: Map<String, String>) : CommonFactory() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getSaksbehandlerGroupIdsByKey(): Map<String, String> {
        val attestantClaim = env["AZUREAD_ATTESTANT_GROUPID"] ?: throw NullPointerException("Mangler attestant claim")
        val saksbehandlerClaim = env["AZUREAD_SAKSBEHANDLER_GROUPID"]
            ?: throw NullPointerException("Mangler saksbehandler claim")
        return mapOf(
            "AZUREAD_ATTESTANT_GROUPID" to attestantClaim,
            "AZUREAD_SAKSBEHANDLER_GROUPID" to saksbehandlerClaim
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

    private val config: Config = ConfigFactory.load()
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
}