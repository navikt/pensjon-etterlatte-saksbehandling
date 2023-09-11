package no.nav.etterlatte.kafka

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.requireEnvValue
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerEgneAnsatte(
    env: Map<String, String>,
    private val behandlingKlient: BehandlingKlient,
    closed: AtomicBoolean,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment(),
    pollTimeoutInSeconds: Duration = Duration.ofSeconds(10L)
) : Kafkakonsument<String>(
    logger = LoggerFactory.getLogger(KafkaConsumerEgneAnsatte::class.java.name),
    consumer = KafkaConsumer<String, String>(kafkaEnvironment.generateKafkaConsumerProperties(env)),
    topic = env.requireEnvValue("SKJERMING_TOPIC"),
    pollTimeoutInSeconds = pollTimeoutInSeconds,
    closed = closed
) {
    override fun stream() {
        stream { meldinger -> meldinger.forEach { behandlingKlient.haandterHendelse(it) } }
    }
}