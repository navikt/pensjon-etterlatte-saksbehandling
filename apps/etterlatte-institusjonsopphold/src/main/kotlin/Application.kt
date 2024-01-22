package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.KafkaConsumerInstitusjonsopphold
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.setReady
import org.slf4j.LoggerFactory

fun main() {
    Server().run()
}

class Server {
    private val defaultConfig: Config = ConfigFactory.load()
    private val engine =
        initEmbeddedServerUtenRest(
            httpPort = 8080,
            applicationConfig = defaultConfig,
        )

    fun run() {
        val env = System.getenv().toMutableMap()
        startInstitusjonsoppholdLytter(env, defaultConfig)
        setReady().also { engine.start(true) }
    }
}

fun startInstitusjonsoppholdLytter(
    env: Map<String, String>,
    config: Config,
) {
    val logger = LoggerFactory.getLogger(Application::class.java)

    val proxyHttpKlient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("azure.proxy.outbound.scope"),
        )

    val institusjonsoppholdKlient = InstitusjonsoppholdKlient(proxyHttpKlient, config.getString("proxy.url"))

    val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        )

    val behandlingKlient =
        BehandlingKlient(
            behandlingHttpClient = behandlingHttpClient,
            institusjonsoppholdKlient = institusjonsoppholdKlient,
            resourceUrl = config.getString("etterlatte.behandling.url"),
        )

    startLytting(
        konsument =
            KafkaConsumerInstitusjonsopphold(
                env = env,
                behandlingKlient = behandlingKlient,
            ),
        logger = logger,
    )
}
