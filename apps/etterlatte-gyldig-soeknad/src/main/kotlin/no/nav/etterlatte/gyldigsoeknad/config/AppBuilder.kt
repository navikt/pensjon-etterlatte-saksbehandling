package no.nav.etterlatte.gyldigsoeknad.config

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(props: Miljoevariabler) {
    fun createBehandlingClient(): BehandlingClient {
        return BehandlingClient(sakOgBehandlingApp, "http://etterlatte-behandling")
    }

    private val sakOgBehandlingApp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = props.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = props.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = props.requireEnvValue("BEHANDLING_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }
}
