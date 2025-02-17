package no.nav.etterlatte.tidshendelser

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.tidshendelser.TidshendelserKey.ETTERLATTE_BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.tidshendelser.TidshendelserKey.HENDELSE_POLLER_INITIAL_DELAY
import no.nav.etterlatte.tidshendelser.TidshendelserKey.HENDELSE_POLLER_INTERVAL
import no.nav.etterlatte.tidshendelser.TidshendelserKey.HENDELSE_POLLER_MAX_ANTALL
import no.nav.etterlatte.tidshendelser.TidshendelserKey.JOBB_POLLER_INITIAL_DELAY
import no.nav.etterlatte.tidshendelser.TidshendelserKey.JOBB_POLLER_INTERVAL
import no.nav.etterlatte.tidshendelser.TidshendelserKey.JOBB_POLLER_OPENING_HOURS
import no.nav.etterlatte.tidshendelser.TidshendelserKey.OPPRETT_JOBBER_INITIAL_DELAY
import no.nav.etterlatte.tidshendelser.TidshendelserKey.OPPRETT_JOBBER_INTERVAL
import no.nav.etterlatte.tidshendelser.TidshendelserKey.OPPRETT_JOBBER_OPENING_HOURS
import no.nav.etterlatte.tidshendelser.aarliginntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.tidshendelser.aldersovergang.AldersovergangerService
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.hendelser.HendelsePoller
import no.nav.etterlatte.tidshendelser.hendelser.HendelsePollerTask
import no.nav.etterlatte.tidshendelser.hendelser.HendelsePublisher
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import no.nav.etterlatte.tidshendelser.omstillingsstoenad.OmstillingsstoenadService
import no.nav.etterlatte.tidshendelser.regulering.ReguleringDao
import no.nav.etterlatte.tidshendelser.regulering.ReguleringService
import java.time.Duration
import java.util.UUID

class AppContext(
    env: Miljoevariabler,
    publisher: (UUID, String) -> Unit,
) {
    private val config: Config = ConfigFactory.load()

    val dataSource = DataSourceBuilder.createDataSource(env)

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(ETTERLATTE_BEHANDLING_AZURE_SCOPE),
        )
    }

    private val grunnlagKlient =
        GrunnlagKlient(
            behandlingHttpClient,
            config.getString("etterlatte.behandling.url"),
        )

    private val behandlingKlient =
        BehandlingKlient(
            behandlingHttpClient,
            config.getString("etterlatte.behandling.url"),
        )

    val hendelseDao = HendelseDao(dataSource)
    private val aldersovergangerService = AldersovergangerService(hendelseDao, grunnlagKlient, behandlingKlient)
    private val omstillingsstoenadService = OmstillingsstoenadService(hendelseDao, grunnlagKlient, behandlingKlient)
    private val reguleringDao = ReguleringDao(dataSource)
    private val reguleringService = ReguleringService(publisher, reguleringDao)
    private val inntektsjusteringService = AarligInntektsjusteringService(publisher, reguleringDao)

    val jobbPollerTask =
        JobbPollerTask(
            initialDelaySeconds = env.requireEnvValue(JOBB_POLLER_INITIAL_DELAY).toLong(),
            periode = env.requireEnvValue(JOBB_POLLER_INTERVAL).let { Duration.parse(it) } ?: Duration.ofMinutes(5),
            openingHours = env.requireEnvValue(JOBB_POLLER_OPENING_HOURS).let { OpeningHours.of(it) },
            jobbPoller =
                JobbPoller(
                    hendelseDao,
                    aldersovergangerService,
                    omstillingsstoenadService,
                    reguleringService,
                    inntektsjusteringService,
                ),
        )

    val jobberTaskScheduler =
        JobbSchedulerTask(
            initialDelaySeconds = env.requireEnvValue(OPPRETT_JOBBER_INITIAL_DELAY).toLong(),
            periode = env.requireEnvValue(OPPRETT_JOBBER_INTERVAL).let { Duration.parse(it) } ?: Duration.ofMinutes(5),
            openingHours = env.requireEnvValue(OPPRETT_JOBBER_OPENING_HOURS).let { OpeningHours.of(it) },
            jobbScheduler = JobbScheduler(hendelseDao),
        )

    val hendelsePollerTask =
        HendelsePollerTask(
            initialDelaySeconds = env.requireEnvValue(HENDELSE_POLLER_INITIAL_DELAY).toLong(),
            periode =
                env.requireEnvValue(HENDELSE_POLLER_INTERVAL).let { Duration.parse(it) }
                    ?: Duration.ofMinutes(5),
            hendelsePoller =
                HendelsePoller(
                    hendelseDao = hendelseDao,
                    hendelsePublisher = HendelsePublisher(publisher),
                ),
            maxAntallHendelsePerPoll = env.requireEnvValue(HENDELSE_POLLER_MAX_ANTALL).toInt(),
        )
}

enum class TidshendelserKey : EnvEnum {
    ETTERLATTE_BEHANDLING_AZURE_SCOPE,
    ETTERLATTE_GRUNNLAG_AZURE_SCOPE,
    HENDELSE_POLLER_INITIAL_DELAY,
    HENDELSE_POLLER_INTERVAL,
    HENDELSE_POLLER_MAX_ANTALL,
    JOBB_POLLER_INITIAL_DELAY,
    JOBB_POLLER_INTERVAL,
    JOBB_POLLER_OPENING_HOURS,
    OPPRETT_JOBBER_INITIAL_DELAY,
    OPPRETT_JOBBER_INTERVAL,
    OPPRETT_JOBBER_OPENING_HOURS,
    ;

    override fun key() = name
}
