package no.nav.etterlatte.hendelserpdl.config

import com.fasterxml.jackson.databind.SerializationFeature
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.hendelserpdl.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.config.PDLKey.LEESAH_TOPIC_PERSON
import no.nav.etterlatte.hendelserpdl.config.PDLKey.PDL_AZURE_SCOPE
import no.nav.etterlatte.hendelserpdl.pdl.PdlTjenesterKlient
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials

class ApplicationContext(
    private val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    private val pdlTjenesterKlient: PdlTjenesterKlient =
        PdlTjenesterKlient(
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = env.requireEnvValue(AzureEnums.AZURE_APP_CLIENT_ID),
                    azureAppJwk = env.requireEnvValue(AzureEnums.AZURE_APP_JWK),
                    azureAppWellKnownUrl = env.requireEnvValue(AzureEnums.AZURE_APP_WELL_KNOWN_URL),
                    azureAppScope = env.requireEnvValue(PDL_AZURE_SCOPE),
                    ekstraJacksoninnstillinger = { it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) },
                ),
            url = "http://etterlatte-pdltjenester",
        ),
    private val personHendelseFordeler: PersonHendelseFordeler =
        PersonHendelseFordeler(
            kafkaProduser = GcpKafkaConfig.fromEnv(env).rapidsAndRiversProducer(env.getValue(KAFKA_RAPID_TOPIC)),
            pdlTjenesterKlient = pdlTjenesterKlient,
        ),
    val leesahKonsument: PersonhendelseKonsument =
        PersonhendelseKonsument(
            env.requireEnvValue(LEESAH_TOPIC_PERSON),
            KafkaEnvironment().generateKafkaConsumerProperties(env),
            personHendelseFordeler,
        ),
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
}

enum class PDLKey : EnvEnum {
    LEESAH_KAFKA_GROUP_ID,
    LEESAH_TOPIC_PERSON,
    PDL_AZURE_SCOPE,
    ;

    override fun key() = name
}
