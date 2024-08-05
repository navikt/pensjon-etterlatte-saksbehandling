package no.nav.etterlatte.statistikk.config

import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEREGNING_AZURE_SCOPE
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BehandlingKlientImpl
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlientImpl
import no.nav.etterlatte.statistikk.database.AktivitetspliktRepo
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.SoeknadStatistikkRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.jobs.MaanedligStatistikkJob
import no.nav.etterlatte.statistikk.river.AktivitetspliktHendelseRiver
import no.nav.etterlatte.statistikk.river.AvbruttOpprettetBehandlinghendelseRiver
import no.nav.etterlatte.statistikk.river.BehandlingPaaVentHendelseRiver
import no.nav.etterlatte.statistikk.river.KlagehendelseRiver
import no.nav.etterlatte.statistikk.river.SoeknadStatistikkRiver
import no.nav.etterlatte.statistikk.river.TilbakekrevinghendelseRiver
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.AktivitetspliktService
import no.nav.etterlatte.statistikk.service.SoeknadStatistikkService
import no.nav.etterlatte.statistikk.service.SoeknadStatistikkServiceImpl
import no.nav.etterlatte.statistikk.service.StatistikkService
import rapidsandrivers.initRogR
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class ApplicationContext {
    private val env = Miljoevariabler.systemEnv()
    private val statistikkService: StatistikkService by lazy {
        StatistikkService(statistikkRepository, sakstatistikkRepository, behandlingKlient, beregningKlient, aktivitetspliktService)
    }

    fun initRapidsConnection() =
        initRogR("statistikk") { rapidsConnection, _ ->
            AvbruttOpprettetBehandlinghendelseRiver(rapidsConnection, statistikkService)
            BehandlingPaaVentHendelseRiver(rapidsConnection, statistikkService)
            TilbakekrevinghendelseRiver(rapidsConnection, statistikkService)
            VedtakhendelserRiver(rapidsConnection, statistikkService)
            SoeknadStatistikkRiver(rapidsConnection, soeknadStatistikkService)
            KlagehendelseRiver(rapidsConnection, statistikkService)
            AktivitetspliktHendelseRiver(rapidsConnection, aktivitetspliktService)
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

    private val soeknadStatistikkRepository: SoeknadStatistikkRepository by lazy {
        SoeknadStatistikkRepository.using(datasource)
    }

    private val soeknadStatistikkService: SoeknadStatistikkService by lazy {
        SoeknadStatistikkServiceImpl(soeknadStatistikkRepository)
    }

    private val aktivitetspliktRepo: AktivitetspliktRepo by lazy {
        AktivitetspliktRepo(datasource)
    }

    private val aktivitetspliktService: AktivitetspliktService by lazy {
        AktivitetspliktService(aktivitetspliktRepo)
    }

    val maanedligStatistikkJob: MaanedligStatistikkJob by lazy {
        MaanedligStatistikkJob(
            statistikkService,
            leaderElection,
            Duration.of(10, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(4, ChronoUnit.HOURS),
        )
    }

    private val leaderElection: LeaderElection by lazy {
        LeaderElection(env[ELECTOR_PATH])
    }

    private val beregningHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(BEREGNING_AZURE_SCOPE),
        )
    }

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(BEHANDLING_AZURE_SCOPE),
        )
    }
}
