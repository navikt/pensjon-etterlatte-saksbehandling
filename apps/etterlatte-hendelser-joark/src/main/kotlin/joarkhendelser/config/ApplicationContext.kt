package no.nav.etterlatte.joarkhendelser.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.joark.SafKlient
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.joarkhendelser.common.JoarkhendelseKonsument
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext(env: Map<String, String> = System.getenv()) {
    private val config: Config = ConfigFactory.load()

    private val behandlingKlient =
        BehandlingKlient(
            httpClientCredentials(config.getString("behandling.azure.scope")),
            config.getString("etterlatte.behandling.url"),
        )

    private val safKlient =
        SafKlient(
            httpClientCredentials(config.getString("saf.azure.scope")),
            config.getString("saf.base.url"),
        )

    private val joarkHendelseHandler =
        JoarkHendelseHandler(
            behandlingKlient,
            safKlient,
        )

    val joarkKonsument =
        JoarkhendelseKonsument(
            env.requireEnvValue("KAFKA_JOARK_HENDELSE_TOPIC"),
            KafkaEnvironment().generateKafkaConsumerProperties(env),
            joarkHendelseHandler,
        )

    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()

    private fun httpClientCredentials(scope: String) =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = scope,
        )
}
