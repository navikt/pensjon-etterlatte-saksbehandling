package no.nav.etterlatte.joarkhendelser.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import joarkhendelser.behandling.BehandlingKlient
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.joarkhendelser.common.JoarkhendelseKonsument
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext(
    private val env: Map<String, String> = System.getenv(),
) {
    private val config: Config = ConfigFactory.load()

    private val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        )

    private val behandlingKlient =
        BehandlingKlient(
            httpClient = behandlingHttpClient,
            url = config.getString("etterlatte.behandling.url"),
        )

    private val joarkHendelseHandler: JoarkHendelseHandler =
        JoarkHendelseHandler(
            BehandlingKlient(
                httpClient = behandlingHttpClient,
                url = config.getString("etterlatte.behandling.url"),
            ),
        )

    val joarkKonsument: JoarkhendelseKonsument =
        JoarkhendelseKonsument(
            env.requireEnvValue("KAFKA_JOARK_HENDELSE_TOPIC"),
            KafkaEnvironment().generateKafkaConsumerProperties(env),
            joarkHendelseHandler,
        )

    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
}
