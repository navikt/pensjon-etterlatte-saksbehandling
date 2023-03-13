package no.nav.etterlatte

import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient

class AppBuilder(private val env: Map<String, String>) {

    internal fun pdlTjenesterKlient() = PdlTjenesterKlient(
        client = pdlTjenesterHttpClient,
        apiUrl = requireNotNull(env["PDL_URL"])
    )

    private val pdlTjenesterHttpClient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("PDL_AZURE_SCOPE")
        )
    }
}