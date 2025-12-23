package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.institusjonsopphold.kafka.KafkaConsumerInstitusjonsopphold
import no.nav.etterlatte.institusjonsopphold.klienter.BehandlingKlient
import no.nav.etterlatte.institusjonsopphold.klienter.InstitusjonsoppholdKlient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext {
    val env = Miljoevariabler.systemEnv()
    val defaultConfig: Config = ConfigFactory.load()

    private val institusjonHttpClient =
        httpClientClientCredentials(
            azureAppClientId = defaultConfig.getString("azure.app.client.id"),
            azureAppJwk = defaultConfig.getString("azure.app.jwk"),
            azureAppWellKnownUrl = defaultConfig.getString("azure.app.well.known.url"),
            azureAppScope = defaultConfig.getString("institusjon.api.scope"),
        )

    val institusjonsoppholdKlient =
        InstitusjonsoppholdKlient(
            institusjonHttpClient,
            defaultConfig.getString("institusjon.api.url"),
        )

    private val behandlingHttpClient =
        httpClientClientCredentials(
            azureAppClientId = defaultConfig.getString("azure.app.client.id"),
            azureAppJwk = defaultConfig.getString("azure.app.jwk"),
            azureAppWellKnownUrl = defaultConfig.getString("azure.app.well.known.url"),
            azureAppScope = defaultConfig.getString("behandling.azure.scope"),
        )

    val behandlingKlient =
        BehandlingKlient(
            behandlingHttpClient = behandlingHttpClient,
            institusjonsoppholdKlient = institusjonsoppholdKlient,
            resourceUrl = defaultConfig.getString("etterlatte.behandling.url"),
        )

    val kafkaConsumerInstitusjonsopphold =
        KafkaConsumerInstitusjonsopphold(
            env = env,
            behandlingKlient = behandlingKlient,
        )
}
