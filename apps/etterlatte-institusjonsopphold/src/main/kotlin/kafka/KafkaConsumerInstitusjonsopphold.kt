package no.nav.etterlatte.kafka

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.requireEnvValue
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerInstitusjonsopphold(
    env: Map<String, String>,
    private var behandlingKlient: BehandlingKlient,
    private val closed: AtomicBoolean,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment(),
    private var pollTimeoutInSeconds: Duration = Duration.ofSeconds(10L)
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val kafkaProperties: Properties = kafkaEnvironment.generateKafkaConsumerProperties(env)
    private val skjermingTopic: String = env.requireEnvValue("SKJERMING_TOPIC")
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(kafkaProperties)
    private var antallMeldinger = 0

    fun getConsumer() = consumer
    fun getAntallMeldinger() = antallMeldinger

    fun stream() {
        try {
            logger.info("Starter KafkaConsumerEgneAnsatte")
            consumer.subscribe(listOf(skjermingTopic))
            while (!closed.get()) {
                val meldinger: ConsumerRecords<String, String> = consumer.poll(pollTimeoutInSeconds)
                meldinger.forEach {
                    behandlingKlient.haandterHendelse(it)
                }
                consumer.commitSync()

                antallMeldinger += meldinger.count()
                if (meldinger.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (!closed.get()) throw e
        } finally {
            logger.info("Lukker KafkaConsumerEgneAnsatte")
            consumer.close()
        }
    }
}