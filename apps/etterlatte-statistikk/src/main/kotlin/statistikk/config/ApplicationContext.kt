package no.nav.etterlatte.statistikk.config

import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BehandlingKlientImpl
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlientImpl
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.river.BehandlinghendelseRiver
import no.nav.etterlatte.statistikk.river.VedtakhendelserRiver
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
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

    private val beregningHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = requireNotNull(env["AZURE_APP_CLIENT_ID"]),
            azureAppJwk = requireNotNull(env["AZURE_APP_JWK"]),
            azureAppWellKnownUrl = requireNotNull(env["AZURE_APP_WELL_KNOWN_URL"]),
            azureAppScope = requireNotNull(env["BEREGNING_AZURE_SCOPE"])
        )
    }

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = requireNotNull(env["AZURE_APP_CLIENT_ID"]),
            azureAppJwk = requireNotNull(env["AZURE_APP_JWK"]),
            azureAppWellKnownUrl = requireNotNull(env["AZURE_APP_WELL_KNOWN_URL"]),
            azureAppScope = requireNotNull(env["BEHANDLING_AZURE_SCOPE"])
        )
    }
}

private fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }