package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.behandling.sak.BehandlingKlient
import no.nav.etterlatte.behandling.sak.BehandlingService
import no.nav.etterlatte.behandling.sak.VedtaksvurderingSakKlient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.oppgave.OppgaveKlient
import no.nav.etterlatte.oppgave.OppgaveService
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

    private val brukVedtakFraBehandling = env[ApiKey.BRUK_VEDTAK_FRA_BEHANDLING] == "ja"

    private val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = config.getString("behandling.outbound"),
        )

    private val vedtakHttpClient by lazy {
        if (brukVedtakFraBehandling) {
            behandlingHttpClient
        } else {
            httpClientClientCredentials(
                azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
                azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
                azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
                azureAppScope = config.getString("vedtak.outbound"),
            )
        }
    }

    private val vedtaksvurderingSamordningKlient =
        VedtaksvurderingSamordningKlient(
            config = config,
            httpClient = vedtakHttpClient,
            brukEtterlatteBehandling = brukVedtakFraBehandling,
        )
    private val vedtaksvurderingSakKlient =
        VedtaksvurderingSakKlient(
            config = config,
            httpClient = vedtakHttpClient,
            brukEtterlatteBehandling = brukVedtakFraBehandling,
        )

    private val tpHttpClient =
        httpClientClientCredentials(
            azureAppClientId = env.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = env.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = env.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = config.getString("tjenestepensjon.outbound"),
        )
    private val tpKlient = TjenestepensjonKlient(config, tpHttpClient)

    val samordningVedtakService = SamordningVedtakService(vedtaksvurderingSamordningKlient, tpKlient)

    private val behandlingKlient = BehandlingKlient(config, behandlingHttpClient)

    val behandlingService = BehandlingService(behandlingKlient, vedtaksvurderingSakKlient)
    val vedtakKlient =
        VedtaksvurderingKlient(
            config = config,
            httpClient = vedtakHttpClient,
            brukEtterlatteBehandling = brukVedtakFraBehandling,
        )
    val vedtakService = VedtakService(vedtakKlient)

    private val oppgaveKlient = OppgaveKlient(config, behandlingHttpClient)
    val oppgaveService = OppgaveService(oppgaveKlient)
}

enum class ApiKey : EnvEnum {
    BRUK_VEDTAK_FRA_BEHANDLING,
    ;

    override fun key() = name
}
