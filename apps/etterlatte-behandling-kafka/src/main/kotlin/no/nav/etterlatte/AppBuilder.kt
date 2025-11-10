package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.DOKARKIV_URL
import no.nav.etterlatte.EnvKey.PDFGEN_URL
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.brukerdialog.omsmeldinnendring.JournalfoerOmsMeldtInnEndringService
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.brukerdialog.soeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.brukerdialog.soeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagKlient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_CLIENT_ID
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_JWK
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_WELL_KNOWN_URL
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.tidshendelser.TidshendelseService
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingServiceImpl

class AppBuilder(
    private val props: Miljoevariabler,
) {
    val behandlingService: BehandlingService by lazy {
        BehandlingServiceImpl(
            behandlingAppExpectSuccess,
            "http://etterlatte-behandling",
        )
    }

    // TODO: Slå sammen med behandlingService?
    val vilkaarsvurderingService: VilkaarsvurderingService by lazy {
        VilkaarsvurderingServiceImpl(
            behandlingAppExpectSuccess,
            "http://etterlatte-behandling",
        )
    }

    val grunnlagKlient: GrunnlagKlient by lazy {
        GrunnlagKlient(behandlingAppExpectSuccess, "http://etterlatte-behandling")
    }

    val tidshendelserService: TidshendelseService by lazy {
        TidshendelseService(
            behandlingService,
        )
    }

    // TODO: Slå sammen med "behandlingService" over
    val behandlingKlient: BehandlingClient by lazy {
        BehandlingClient(behandlingAppExpectSuccess, "http://etterlatte-behandling")
    }

    val journalfoerSoeknadService: JournalfoerSoeknadService by lazy {
        JournalfoerSoeknadService(
            DokarkivKlient(
                httpClient(EnvKey.DOKARKIV_SCOPE),
                props.requireEnvValue(DOKARKIV_URL),
            ),
            PdfGeneratorKlient(httpClient(), "${props.requireEnvValue(PDFGEN_URL)}/eypdfgen"),
        )
    }

    val journalfoerOmsMeldtInnEndringService: JournalfoerOmsMeldtInnEndringService by lazy {
        JournalfoerOmsMeldtInnEndringService(
            DokarkivKlient(
                httpClient(EnvKey.DOKARKIV_SCOPE),
                props.requireEnvValue(DOKARKIV_URL),
            ),
            PdfGeneratorKlient(httpClient(), "${props.requireEnvValue(PDFGEN_URL)}/omsendringer"),
        )
    }

    private val behandlingAppExpectSuccess: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = props.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = props.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = props.requireEnvValue(BEHANDLING_AZURE_SCOPE),
        )
    }

    val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))

    private fun httpClient(scope: EnvEnum) =
        httpClientClientCredentials(
            forventSuksess = false,
            azureAppClientId = props.requireEnvValue(AZURE_APP_CLIENT_ID),
            azureAppJwk = props.requireEnvValue(AZURE_APP_JWK),
            azureAppWellKnownUrl = props.requireEnvValue(AZURE_APP_WELL_KNOWN_URL),
            azureAppScope = props.requireEnvValue(scope),
        )
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
