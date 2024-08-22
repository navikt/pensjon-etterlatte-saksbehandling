package no.nav.etterlatte.samordning

import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.hentKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.samordning.SamordningKey.SAMORDNINGVEDTAK_HENDELSE_TOPIC

class ApplicationContext(
    env: Miljoevariabler = Miljoevariabler.systemEnv(),
) {
    private val handler =
        SamordningHendelseHandler(
            kafkaProduser =
                hentKafkaConfig(env)
                    .rapidsAndRiversProducer(env.requireEnvValue(KAFKA_RAPID_TOPIC)),
        )

    val konsument =
        SamordningHendelseKonsument(
            topic = env.requireEnvValue(SAMORDNINGVEDTAK_HENDELSE_TOPIC),
            kafkaProperties = KafkaEnvironment().generateKafkaConsumerProperties(env),
            handler = handler,
        )

    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
}

enum class SamordningKey : EnvEnum {
    SAMORDNINGVEDTAK_HENDELSE_GROUP_ID,
    SAMORDNINGVEDTAK_HENDELSE_TOPIC,
    ;

    override fun key() = name
}
