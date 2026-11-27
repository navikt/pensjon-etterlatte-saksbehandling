package no.nav.etterlatte.beregningkafka

import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEHANDLING_URL
import no.nav.etterlatte.EnvKey.BEREGNING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEREGNING_URL
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(
    private val props: Miljoevariabler,
) {
    private val brukInternBeregning = props.getOrDefault(BRUK_INTERN_BEREGNING, "false").toBooleanStrict()

    private val beregningapp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = props.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = props.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = props.requireEnvValue(if (brukInternBeregning) BEHANDLING_AZURE_SCOPE else BEREGNING_AZURE_SCOPE),
        )
    }

    fun createBeregningService(): BeregningService =
        BeregningService(
            beregningapp,
            props.requireEnvValue(if (brukInternBeregning) BEHANDLING_URL else BEREGNING_URL),
        )
}
