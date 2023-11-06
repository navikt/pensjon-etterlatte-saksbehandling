package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.requireEnvValue

class ApplicationContext(env: Map<String, String> = System.getenv()) {
    private val handler =
        SamordningHendelseHandler(
            kafkaProduser =
                GcpKafkaConfig.fromEnv(env)
                    .rapidsAndRiversProducer(env.getValue("KAFKA_RAPID_TOPIC")),
        )

    val konsument =
        SamordningHendelseKonsument(
            topic = env.requireEnvValue("SAMORDNINGVEDTAK_HENDELSE_TOPIC"),
            kafkaProperties = KafkaEnvironment().generateKafkaConsumerProperties(env),
            handler = handler,
        )

    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
}
