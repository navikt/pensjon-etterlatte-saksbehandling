package no.nav.etterlatte.hendelserufoere.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.hendelserufoere.UfoereHendelseFordeler
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.hendelserufoere.config.UfoereKey.UFOERE_TOPIC
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext(
    private val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    private val config: Config = ConfigFactory.load(),
    private val behandlingHttpClient: HttpClient =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        ),
    val behandlingKlient: BehandlingKlient =
        BehandlingKlient(
            behandlingHttpClient = behandlingHttpClient,
            resourceUrl = config.getString("etterlatte.behandling.url"),
        ),
    private val ufoereHendelseFordeler: UfoereHendelseFordeler = UfoereHendelseFordeler(behandlingKlient),
    val ufoereKonsument: UfoerehendelseKonsument =
        UfoerehendelseKonsument(
            env.requireEnvValue(UFOERE_TOPIC),
            KafkaEnvironment().generateKafkaConsumerProperties(env),
            ufoereHendelseFordeler,
        ),
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
}

enum class UfoereKey : EnvEnum {
    UFOERE_KAFKA_GROUP_ID,
    UFOERE_TOPIC,
    ;

    override fun key() = name
}
