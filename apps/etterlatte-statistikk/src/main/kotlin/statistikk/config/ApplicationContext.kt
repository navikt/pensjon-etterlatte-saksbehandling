package no.nav.etterlatte.statistikk.config

import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BehandlingKlientImpl
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlientImpl
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.jobs.MaanedligStatistikkJob
import no.nav.etterlatte.statistikk.river.BehandlinghendelseRiver
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class ApplicationContext {
    private val env = System.getenv()
    val rapidsConnection: RapidsConnection = RapidApplication.create(env.withConsumerGroupId())

    private val statistikkService: StatistikkService by lazy {
        StatistikkService(statistikkRepository, sakstatistikkRepository, behandlingKlient, beregningKlient)
    }

    val behandlinghendelseRiver: BehandlinghendelseRiver by lazy {
        BehandlinghendelseRiver(rapidsConnection, statistikkService)
    }

    val vedtakhendelserRiver: VedtakhendelserRiver by lazy {
        VedtakhendelserRiver(rapidsConnection, statistikkService)
    }

    private val behandlingKlient: BehandlingKlient by lazy {
        BehandlingKlientImpl(behandlingHttpClient, "http://etterlatte-behandling")
    }

    private val statistikkRepository: StoenadRepository by lazy {
        StoenadRepository(datasource)
    }

    private val sakstatistikkRepository: SakRepository by lazy {
        SakRepository(datasource)
    }
    private val datasource: DataSource by lazy { DataSourceBuilder.createDataSource(env).also { it.migrate() } }

    private val beregningKlient: BeregningKlient by lazy {
        BeregningKlientImpl(beregningHttpClient, "http://etterlatte-beregning")
    }

    val maanedligStatistikkJob: MaanedligStatistikkJob by lazy {
        MaanedligStatistikkJob(
            statistikkService,
            leaderElection,
            Duration.of(10, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(4, ChronoUnit.HOURS)
        )
    }

    private val leaderElection: LeaderElection by lazy {
        LeaderElection(env.requireEnvValue("ELECTOR_PATH"))
    }

    private val beregningHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("BEREGNING_AZURE_SCOPE")
        )
    }

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("BEHANDLING_AZURE_SCOPE")
        )
    }
}

private fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("app env is missing required key $key")
        else -> value
    }

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }