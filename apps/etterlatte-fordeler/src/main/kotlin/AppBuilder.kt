package no.nav.etterlatte

import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import rapidsandrivers.RapidsAndRiversAppBuilder

class AppBuilder(private val env: Miljoevariabler) : RapidsAndRiversAppBuilder(env) {

    internal fun pdlTjenesterKlient() = PdlTjenesterKlient(
        client = pdlTjenesterHttpClient,
        apiUrl = env.requireEnvValue("PDL_URL")
    )

    internal fun behandlingKlient() = BehandlingKlient(
        httpClient = behandlingHttpClient,
        url = env.requireEnvValue("BEHANDLING_URL")
    )

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("BEHANDLING_AZURE_SCOPE")
        )
    }

    private val pdlTjenesterHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("PDL_AZURE_SCOPE")
        )
    }

    fun longFeature(featureName: String, default: Long = 0): Long {
        return (env[featureName]?.toLong() ?: default).takeIf { it > -1 } ?: Long.MAX_VALUE
    }

    fun createDataSource() = DataSourceBuilder.createDataSource(env.props).apply { migrate() }
}