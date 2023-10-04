package no.nav.etterlatte.samordning

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.samordning.vedtak.SamordningVedtakService
import no.nav.etterlatte.samordning.vedtak.TjenestepensjonKlient
import no.nav.etterlatte.samordning.vedtak.VedtaksvurderingKlient

class ApplicationContext(env: Miljoevariabler) {
    val config: Config = ConfigFactory.load()
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()

    private val vedtaksvurderingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("ETTERLATTE_VEDTAKSVURDERING_SCOPE"),
        )
    private val vedtaksvurderingAzureAdClient = AzureAdClient(config, vedtaksvurderingHttpClient)
    private val vedtaksvurderingKlient =
        VedtaksvurderingKlient(
            config,
            vedtaksvurderingHttpClient,
            vedtaksvurderingAzureAdClient,
        )

    private val tpHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue("AZURE_APP_CLIENT_ID"),
            azureAppJwk = env.requireEnvValue("AZURE_APP_JWK"),
            azureAppWellKnownUrl = env.requireEnvValue("AZURE_APP_WELL_KNOWN_URL"),
            azureAppScope = env.requireEnvValue("TJENESTEPENSJON_SCOPE"),
        )
    private val tpAzureAdClient = AzureAdClient(config, tpHttpClient)
    private val tpKlient = TjenestepensjonKlient(config, tpHttpClient, tpAzureAdClient)

    val samordningVedtakService = SamordningVedtakService(vedtaksvurderingKlient, tpKlient)
}
