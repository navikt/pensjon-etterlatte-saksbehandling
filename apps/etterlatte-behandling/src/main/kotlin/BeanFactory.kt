package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingsHendelser
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.RealGenerellBehandlingService
import no.nav.etterlatte.behandling.common.LeaderElection
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingFactory
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.foerstegangsbehandling.RealFoerstegangsbehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.behandling.manueltopphoer.RealManueltOpphoerService
import no.nav.etterlatte.behandling.revurdering.RealRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingFactory
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.grunnlagsendring.GrunnlagClient
import no.nav.etterlatte.grunnlagsendring.GrunnlagClientImpl
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseJob
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaConfig
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdl.PdlService
import no.nav.etterlatte.pdl.PdlServiceImpl
import no.nav.etterlatte.sak.RealSakService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.security.ktor.clientCredential
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
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
    fun pdlService(): PdlService
    fun leaderElection(): LeaderElection
    fun grunnlagsendringshendelseJob(): Timer
    fun grunnlagHttpClient(): HttpClient
    fun grunnlagClient(): GrunnlagClient
}

abstract class CommonFactory : BeanFactory {
    private val behandlingsHendelser: BehandlingsHendelser by lazy {
        BehandlingsHendelser(
            rapid(),
            behandlingDao(),
            datasourceBuilder().dataSource,
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

    override fun generellBehandlingService(): GenerellBehandlingService =
        RealGenerellBehandlingService(
            behandlingDao(),
            behandlingHendelser().nyHendelse,
            foerstegangsbehandlingFactory(),
            revurderingFactory(),
            hendelseDao(),
            manueltOpphoerService()
        )

    override fun sakDao(): SakDao = SakDao { databaseContext().activeTx() }
    override fun behandlingDao(): BehandlingDao = BehandlingDao { databaseContext().activeTx() }
    override fun hendelseDao(): HendelseDao = HendelseDao { databaseContext().activeTx() }
    override fun grunnlagsendringshendelseDao(): GrunnlagsendringshendelseDao =
        GrunnlagsendringshendelseDao { databaseContext().activeTx() }

    override fun pdlService() = PdlServiceImpl(pdlHttpClient(), "http://etterlatte-pdltjenester")

    override fun grunnlagClient() = GrunnlagClientImpl(grunnlagHttpClient())

    override fun grunnlagsendringshendelseService(): GrunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            grunnlagsendringshendelseDao(),
            generellBehandlingService(),
            pdlService(),
            grunnlagClient()
        )

    override fun grunnlagsendringshendelseJob() = GrunnlagsendringshendelseJob(
        datasource = datasourceBuilder().dataSource,
        grunnlagsendringshendelseService = grunnlagsendringshendelseService(),
        leaderElection = leaderElection(),
        initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
        periode = Duration.of(1, ChronoUnit.MINUTES),
        minutterGamleHendelser = 1L
    ).schedule()
}

class EnvBasedBeanFactory(val env: Map<String, String>) : CommonFactory() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val datasourceBuilder: DataSourceBuilder by lazy { DataSourceBuilder(env) }
    override fun datasourceBuilder() = datasourceBuilder

    override fun rapid(): KafkaProdusent<String, String> {
        return kafkaConfig().standardProducer(env.getValue("KAFKA_RAPID_TOPIC"))
    }

    private fun kafkaConfig(): KafkaConfig = GcpKafkaConfig.fromEnv(env)

    override fun pdlHttpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(Auth) {
            clientCredential {
                config = env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
            }
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

    override fun leaderElection() = LeaderElection(env.getValue("ELECTOR_PATH"))
    override fun grunnlagHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        install(Auth) {
            clientCredential {
                config = env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("GRUNNLAG_AZURE_SCOPE"))) }
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
            url("http://etterlatte-grunnlag/api/")
        }
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
            datasource = datasourceBuilder().dataSource,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService(),
            leaderElection = leaderElection(),
            initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(env.getValue("HENDELSE_JOB_FREKVENS").toLong(), ChronoUnit.MINUTES),
            minutterGamleHendelser = env.getValue("HENDELSE_MINUTTER_GAMLE_HENDELSER").toLong()
        ).schedule()
    }
}