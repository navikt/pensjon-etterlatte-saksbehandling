package no.nav.etterlatte.klage

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule

val sikkerLogg = sikkerlogger()

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    private val env = System.getenv()
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()

    val featureToggleService =
        FeatureToggleService.initialiser(
            properties =
                FeatureToggleProperties(
                    applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
                    host = config.getString("funksjonsbrytere.unleash.host"),
                    apiKey = config.getString("funksjonsbrytere.unleash.token"),
                ),
        )

    private val kabalHttpClient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("kabal.azure.scope"),
        )

    private val kabalKlient = KabalKlientImpl(client = kabalHttpClient, kabalUrl = config.getString("kabal.resource.url"))

    val kabalOversendelseService: KabalOversendelseService = KabalOversendelseServiceImpl(kabalKlient = kabalKlient)

    private val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        )

    private val behandlingKlient: BehandlingKlient = BehandlingKlient(behandlingHttpClient, "")

    val kabalKafkakonsument: KlageKafkakonsument =
        KlageKafkakonsument(
            env = env,
            topic = env.requireEnvValue("KLAGE_TOPIC"),
            behandlingKlient = behandlingKlient,
        )
}

fun Application.module(context: ApplicationContext) {
    with(context) {
        restModule(
            sikkerLogg = sikkerLogg,
        ) {
            kabalOvesendelseRoute(
                kabalOversendelseService = kabalOversendelseService,
                featureToggleService = featureToggleService,
            )
        }
    }
}
