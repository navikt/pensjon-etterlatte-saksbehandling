package no.nav.etterlatte.tidshendelser

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import java.time.Duration

class AppContext(env: Miljoevariabler) {
    private val config: Config = ConfigFactory.load()
    private val clock = utcKlokke()

    val dataSource = DataSourceBuilder.createDataSource(env.props)

    private val grunnlagHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("ETTERLATTE_GRUNNLAG_SCOPE"),
        )
    }

    private val grunnlagKlient =
        GrunnlagKlient(
            grunnlagHttpClient,
            config.getString("etterlatte.grunnlag.url"),
        )

    private val hendelseDao = HendelseDao(dataSource)

    val aldersovergangerService =
        AldersovergangerService(
            hendelseDao,
            grunnlagKlient,
        )

    private val jobbRunner = JobbRunner(hendelseDao, aldersovergangerService)

    val jobbPoller =
        JobbPoller(
            leaderElection = LeaderElection(electorPath = env.maybeEnvValue("ELECTOR_PATH")),
            initialDelaySeconds = env.maybeEnvValue("JOBB_POLLER_INITIAL_DELAY")?.toLong() ?: 60L,
            periode = env.maybeEnvValue("JOBB_POLLER_INTERVAL")?.let { Duration.parse(it) } ?: Duration.ofMinutes(5),
            clock = clock,
            jobbRunner = jobbRunner,
        )
}
