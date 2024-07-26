package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AppConfig.HTTP_PORT

class ApplicationContext(
    env: Miljoevariabler = Miljoevariabler.systemEnv(),
) {
    private val handler =
        SamordningHendelseHandler(
            kafkaProduser =
                GcpKafkaConfig
                    .fromEnv(env)
                    .rapidsAndRiversProducer(env.getValue(KAFKA_RAPID_TOPIC)),
        )

    val konsument =
        SamordningHendelseKonsument(
            topic = env.requireEnvValue("SAMORDNINGVEDTAK_HENDELSE_TOPIC"),
            kafkaProperties = KafkaEnvironment().generateKafkaConsumerProperties(env),
            handler = handler,
        )

    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
}
