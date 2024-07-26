package no.nav.etterlatte.joarkhendelser.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingKlient
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingService
import no.nav.etterlatte.joarkhendelser.common.JoarkhendelseKonsument
import no.nav.etterlatte.joarkhendelser.config.JoarkKey.KAFKA_JOARK_HENDELSE_TOPIC
import no.nav.etterlatte.joarkhendelser.joark.SafKlient
import no.nav.etterlatte.joarkhendelser.oppgave.OppgaveKlient
import no.nav.etterlatte.joarkhendelser.pdl.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AppConfig.HTTP_PORT
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext(
    env: Miljoevariabler = Miljoevariabler.systemEnv(),
) {
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

    private val pdlTjenesterKlient =
        PdlTjenesterKlient(
            httpClientCredentials(config.getString("pdl.azure.scope")),
            config.getString("pdl.base.url"),
        )

    private val oppgaveKlient =
        OppgaveKlient(
            httpClientCredentials(config.getString("oppgave.azure.scope")),
            config.getString("oppgave.base.url"),
        )

    private val joarkHendelseHandler =
        JoarkHendelseHandler(
            BehandlingService(behandlingKlient),
            safKlient,
            oppgaveKlient,
            pdlTjenesterKlient,
        )

    val joarkKonsument =
        JoarkhendelseKonsument(
            env.requireEnvValue(KAFKA_JOARK_HENDELSE_TOPIC),
            KafkaEnvironment().generateKafkaConsumerProperties(env),
            joarkHendelseHandler,
        )

    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()

    private fun httpClientCredentials(scope: String) =
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = scope,
        )
}

enum class JoarkKey : EnvEnum {
    KAFKA_JOARK_HENDELSE_TOPIC,
    ;

    override fun name() = name
}
