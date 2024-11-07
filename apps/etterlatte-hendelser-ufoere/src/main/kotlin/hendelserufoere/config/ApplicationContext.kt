package no.nav.etterlatte.hendelserufoere.config

import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.hendelserufoere.UfoereHendelseFordeler
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.hendelserufoere.config.UfoereKey.UFOERE_TOPIC
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler

class ApplicationContext(
    private val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    private val ufoereHendelseFordeler: UfoereHendelseFordeler =
        UfoereHendelseFordeler(
            kafkaProduser = GcpKafkaConfig.fromEnv(env).rapidsAndRiversProducer(env.requireEnvValue(KAFKA_RAPID_TOPIC)),
        ),
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
