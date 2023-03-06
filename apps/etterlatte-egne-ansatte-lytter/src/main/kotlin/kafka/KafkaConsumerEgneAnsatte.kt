package no.nav.etterlatte.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import no.nav.etterlatte.BehandlingKlient
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class KafkaConsumerEgneAnsatte(
    env: Map<String, String>,
    private var behandlingKlient: BehandlingKlient,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val kafkaProperties: Properties = kafkaEnvironment.generateKafkaConsumerProperties(env)
    private val skjermingTopic: String = env["SKJERMING_TOPIC"]!!
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(kafkaProperties)
    private var antallMeldinger = 0

    suspend fun poll() {
        consumer.use {
            consumer.subscribe(listOf(skjermingTopic))
            logger.info("KafkaConsumerEgneAnsatte startet")
            while (true) {
                val meldinger: ConsumerRecords<String, String> = consumer.poll(Duration.ofSeconds(10L))
                meldinger.forEach {
                    behandlingKlient.haandterHendelse(it)
                }
                try {
                    consumer.commitSync()
                } catch (e: Exception) {
                    logger.error("Kunne ikke committe offsett")
                    throw e
                }
                antallMeldinger = meldinger.count()
                logger.info("En runde med $antallMeldinger")
                if (antallMeldinger == 0) {
                    delay(500)
                }
                yield()
            }
        }
    }
}