package no.nav.etterlatte.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.requireEnvValue
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class KafkaConsumerEgneAnsatte(
    env: Map<String, String>,
    private var behandlingKlient: BehandlingKlient,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment(),
    private var pollTimeoutInSeconds: Duration = Duration.ofSeconds(8L)
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val kafkaProperties: Properties = kafkaEnvironment.generateKafkaConsumerProperties(env)
    private val skjermingTopic: String = env.requireEnvValue("SKJERMING_TOPIC")
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(kafkaProperties).also {
        it.subscribe(listOf(skjermingTopic))
    }

    suspend fun poll() {
        consumer.use {
            logger.info("KafkaConsumerEgneAnsatte startet")
            while (true) {
                val meldinger: ConsumerRecords<String, String> = consumer.poll(pollTimeoutInSeconds)
                meldinger.forEach {
                    behandlingKlient.haandterHendelse(it)
                }
                try {
                    consumer.commitSync()
                } catch (e: Exception) {
                    logger.error("Kunne ikke committe offsett")
                    throw e
                }
                val antallMeldinger = meldinger.count()
                logger.info("En runde med $antallMeldinger")
                if (antallMeldinger == 0) {
                    delay(500)
                }
                yield()
            }
        }
    }
}