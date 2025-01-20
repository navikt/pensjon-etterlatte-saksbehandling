package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.behandling.sak.BehandlingKlient
import no.nav.etterlatte.behandling.sak.BehandlingService
import no.nav.etterlatte.behandling.sak.VedtaksvurderingSakKlient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.samordning.vedtak.SamordningVedtakService
import no.nav.etterlatte.samordning.vedtak.TjenestepensjonKlient
import no.nav.etterlatte.samordning.vedtak.VedtaksvurderingSamordningKlient
import no.nav.etterlatte.vedtak.VedtakService
import no.nav.etterlatte.vedtak.VedtaksvurderingKlient

class ApplicationContext(
    env: Miljoevariabler,
) {
    val config: Config = ConfigFactory.load()
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()

    private val vedtaksvurderingHttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = config.getString("vedtak.outbound"),
        )
    }
    private val vedtaksvurderingSamordningKlient = VedtaksvurderingSamordningKlient(config, vedtaksvurderingHttpClient)
    private val vedtaksvurderingSakKlient = VedtaksvurderingSakKlient(config, vedtaksvurderingHttpClient)

    private val tpHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = config.getString("tjenestepensjon.outbound"),
        )
    private val tpKlient = TjenestepensjonKlient(config, tpHttpClient)

    val samordningVedtakService = SamordningVedtakService(vedtaksvurderingSamordningKlient, tpKlient)

    private val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = config.getString("behandling.outbound"),
        )
    private val behandlingKlient = BehandlingKlient(config, behandlingHttpClient)

    val behandlingService = BehandlingService(behandlingKlient, vedtaksvurderingSakKlient)
    val vedtakKlient = VedtaksvurderingKlient(config, vedtaksvurderingHttpClient)
    val vedtakService = VedtakService(vedtakKlient)
}
