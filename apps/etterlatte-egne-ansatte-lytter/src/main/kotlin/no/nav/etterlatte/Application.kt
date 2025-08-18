package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.EgneAnsatteLytterKey.SKJERMING_TOPIC
import no.nav.etterlatte.kafka.BehandlingKlient
import no.nav.etterlatte.kafka.KafkaConsumerEgneAnsatte
import no.nav.etterlatte.kafka.KafkaEnvironment
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import org.slf4j.LoggerFactory

fun main() {
    Server().runServer()
}

class Server {
    private val defaultConfig: Config = ConfigFactory.load()
    private val engine = initEmbeddedServerUtenRest(httpPort = 8080, applicationConfig = defaultConfig)

    init {
        sikkerLoggOppstart("etterlatte-egne-ansatte-lytter")
    }

    fun runServer() {
        val env = Miljoevariabler.systemEnv()
        startEgenAnsattLytter(env, defaultConfig)
        engine.runEngine()
    }
}

fun startEgenAnsattLytter(
    env: Miljoevariabler,
    config: Config,
) {
    val logger = LoggerFactory.getLogger(Application::class.java)

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
            url = config.getString("etterlatte.behandling.url"),
        )

    startLytting(
        konsument =
            KafkaConsumerEgneAnsatte(
                topic = env.requireEnvValue(SKJERMING_TOPIC),
                kafkaProperties = KafkaEnvironment().generateKafkaConsumerProperties(env),
                behandlingKlient = behandlingKlient,
            ),
        logger = logger,
    )
}

enum class EgneAnsatteLytterKey : EnvEnum {
    SKJERMING_GROUP_ID,
    SKJERMING_TOPIC,
    ;

    override fun key() = name
}
