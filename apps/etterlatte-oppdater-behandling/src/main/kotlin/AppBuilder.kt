package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(
    props: Miljoevariabler,
) {
    val behandlingService: BehandlingService by lazy {
        BehandlingServiceImpl(
            behandlingApp,
            "http://etterlatte-behandling",
        )
    }

    val tidshendelserService: TidshendelseService by lazy {
        TidshendelseService(
            behandlingService,
        )
    }

    private val behandlingApp: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = props.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = props.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = props.requireEnvValue("BEHANDLING_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }

    val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
