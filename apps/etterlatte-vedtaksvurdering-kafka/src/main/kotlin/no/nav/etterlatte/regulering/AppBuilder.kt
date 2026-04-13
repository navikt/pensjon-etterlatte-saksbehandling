package no.nav.etterlatte.regulering

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.UTBETALING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.VEDTAK_AZURE_SCOPE
import no.nav.etterlatte.VedtakServiceImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BrevKlient
import no.nav.etterlatte.klienter.UtbetalingKlient
import no.nav.etterlatte.klienter.UtbetalingKlientImpl
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.regulering.VedtakKafkaKey.BRUK_VEDTAK_FRA_BEHANDLING
import no.nav.etterlatte.regulering.VedtakKafkaKey.ETTERLATTE_BEHANDLING_URL
import no.nav.etterlatte.regulering.VedtakKafkaKey.ETTERLATTE_BREV_API_URL
import no.nav.etterlatte.regulering.VedtakKafkaKey.ETTERLATTE_UTBETALING_URL
import no.nav.etterlatte.regulering.VedtakKafkaKey.ETTERLATTE_VEDTAK_URL

class AppBuilder(
    props: Miljoevariabler,
) {
    private val behandlingUrl = krevIkkeNull(props[ETTERLATTE_BEHANDLING_URL]) { "Mangler behandling url " }
    private val vedtakUrl = krevIkkeNull(props[ETTERLATTE_VEDTAK_URL]) { "Mangler vedtak url " }
    private val utbetalingUrl = krevIkkeNull(props[ETTERLATTE_UTBETALING_URL]) { "Mangler utbetaling url " }
    private val brevUrl = krevIkkeNull(props[ETTERLATTE_BREV_API_URL]) { "Mangler brev-api url " }
    private val brukVedtakFraBehandling = krevIkkeNull(props[BRUK_VEDTAK_FRA_BEHANDLING]) { "Mangler toggle " } == "ja"
    private val env = Miljoevariabler.systemEnv()

    fun lagVedtakKlient(): VedtakServiceImpl =
        if (brukVedtakFraBehandling) {
            VedtakServiceImpl(behandlingHttpKlient, behandlingUrl)
        } else {
            VedtakServiceImpl(vedtakHttpKlient, vedtakUrl)
        }

    fun lagUtbetalingKlient(): UtbetalingKlient = UtbetalingKlientImpl(utbetalingHttpKlient, utbetalingUrl)

    fun lagBrevKlient(): BrevKlient = BrevKlient(brevHttpKlient, brevUrl)

    fun lagFeatureToggleService(): FeatureToggleService = FeatureToggleService.initialiser(featureToggleProperties)

    private val behandlingHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(BEHANDLING_AZURE_SCOPE),
        )
    }

    private val vedtakHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(VEDTAK_AZURE_SCOPE),
        )
    }

    private val utbetalingHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(UTBETALING_AZURE_SCOPE),
        )
    }

    private val brevHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = env.requireEnvValue(EnvKey.BREV_AZURE_SCOPE),
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

enum class VedtakKafkaKey : EnvEnum {
    ETTERLATTE_BEHANDLING_URL,
    ETTERLATTE_VEDTAK_URL,
    ETTERLATTE_UTBETALING_URL,
    ETTERLATTE_BREV_API_URL,
    BRUK_VEDTAK_FRA_BEHANDLING,
    ;

    override fun key() = name
}
