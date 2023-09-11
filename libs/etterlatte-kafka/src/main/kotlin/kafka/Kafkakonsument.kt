package no.nav.etterlatte.kafka

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

abstract class Kafkakonsument<T>(
    val logger: Logger,
    val consumer: KafkaConsumer<String, T>,
    val topic: String,
    val pollTimeoutInSeconds: Duration,
    val closed: AtomicBoolean
) {

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                closed.set(true)
                consumer.wakeup(); // trådsikker, avbryter konsumer fra polling
            }
        )
    }

    private var antallMeldinger = 0
    fun getAntallMeldinger() = antallMeldinger

    abstract fun stream()

    fun stream(haandter: (ConsumerRecords<String, T>) -> Unit) {
        try {
            logger.info("Starter å lese hendelser fra ${this.javaClass.name}")
            consumer.subscribe(listOf(topic))
            while (!closed.get()) {
                val meldinger: ConsumerRecords<String, T> = consumer.poll(pollTimeoutInSeconds)
                haandter(meldinger)
                consumer.commitSync()

                antallMeldinger += meldinger.count()
                if (meldinger.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (!closed.get()) throw e
        } finally {
            logger.info("Ferdig med å lese hendelser fra $${this.javaClass.name} - lukker consumer")
            consumer.close()
        }
    }
}