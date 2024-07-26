package no.nav.etterlatte.klage

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.klage.KlageKey.KLAGE_TOPIC
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

val sikkerLogg = sikkerlogger()

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    private val env = Miljoevariabler.systemEnv()
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()

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

    private val behandlingKlient: BehandlingKlient = BehandlingKlient(behandlingHttpClient, config.getString("behandling.resource.url"))

    val kabalKafkakonsument: KlageKafkakonsument =
        KlageKafkakonsument(
            env = env,
            topic = env.requireEnvValue(KLAGE_TOPIC),
            behandlingKlient = behandlingKlient,
        )
}

enum class KlageKey : EnvEnum {
    KLAGE_TOPIC,
    ;

    override fun name() = name
}
