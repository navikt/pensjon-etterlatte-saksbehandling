package no.nav.etterlatte.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerEgneAnsatte(
    topic: String,
    private val behandlingKlient: BehandlingKlient,
    kafkaProperties: Properties,
    pollTimeoutInSeconds: Duration = Duration.ofSeconds(10L),
    closed: AtomicBoolean = AtomicBoolean(false),
) : Kafkakonsument<String>(
        logger = LoggerFactory.getLogger(KafkaConsumerEgneAnsatte::class.java.name),
        consumer = KafkaConsumer<String, String>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = pollTimeoutInSeconds,
        closed = closed,
    ) {
    override fun stream() {
        stream { meldinger -> meldinger.forEach { behandlingKlient.haandterHendelse(it) } }
    }
}
