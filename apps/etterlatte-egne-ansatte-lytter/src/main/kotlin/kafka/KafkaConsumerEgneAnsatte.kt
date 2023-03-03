package no.nav.etterlatte.kafka

import kotlinx.coroutines.delay
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
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(kafkaProperties).also {
        it.subscribe(listOf("nom.skjermede-personer-status-v1"))
    }
    private var antallMeldinger = 0
    private val skjermingTopic: String = env["SKJERMING_TOPIC"]!!

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

                if (antallMeldinger == 0) {
                    delay(500)
                }
            }
        }
    }
}