package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.VedtakServiceImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class AppBuilder(
    props: Miljoevariabler,
) {
    private val vedtakUrl = requireNotNull(props["ETTERLATTE_VEDTAK_URL"]) { "Mangler vedtak url " }
    private val env = Miljoevariabler.systemEnv()

    fun lagVedtakKlient(): VedtakServiceImpl = VedtakServiceImpl(vedtakHttpKlient, vedtakUrl)

    fun lagFeatureToggleService(): FeatureToggleService = FeatureToggleService.initialiser(featureToggleProperties)

    private val vedtakHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("VEDTAK_AZURE_SCOPE"),
            ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
        )
    }

    private val featureToggleProperties: FeatureToggleProperties by lazy {
        featureToggleProperties(ConfigFactory.load())
    }
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
