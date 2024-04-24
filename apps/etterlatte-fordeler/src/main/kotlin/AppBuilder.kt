package no.nav.etterlatte

import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(private val env: Miljoevariabler) {
    internal fun behandlingKlient() =
        BehandlingKlient(
            httpClient = behandlingHttpClient,
            url = env.requireEnvValue("BEHANDLING_URL"),
        )

    private val behandlingHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("BEHANDLING_AZURE_SCOPE"),
        )
    }
}
