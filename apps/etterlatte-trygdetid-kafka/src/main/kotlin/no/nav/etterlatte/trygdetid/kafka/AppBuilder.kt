package no.nav.etterlatte.trygdetid.kafka

import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey
import no.nav.etterlatte.EnvKey.TRYGDETID_AZURE_SCOPE
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(
    props: Miljoevariabler,
) {
    val brukBehandlingForTrygdetid = props.requireEnvValue(EnvKey.ETTERLATTE_BEHANDLING_TRYGDETID_ENABLED) == "true"
    private val trygdetidapp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = props.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = props.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope =
                if (brukBehandlingForTrygdetid) {
                    props.requireEnvValue(EnvKey.BEHANDLING_AZURE_SCOPE)
                } else {
                    props.requireEnvValue(
                        TRYGDETID_AZURE_SCOPE,
                    )
                },
        )
    }

    fun createTrygdetidService(): TrygdetidService =
        TrygdetidService(
            trygdetidapp,
            if (brukBehandlingForTrygdetid) "http://etterlatte-behandling" else "http://etterlatte-trygdetid",
        )
}
