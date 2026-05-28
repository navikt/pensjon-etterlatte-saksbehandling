package no.nav.etterlatte.kafka

import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

abstract class Kafkakonsument<K, T>(
    val logger: Logger,
    val consumer: KafkaConsumer<K, T>,
    val topic: String,
    val pollTimeoutInSeconds: Duration,
    protected val closed: AtomicBoolean = AtomicBoolean(false),
) {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                closed.set(true)
                consumer.wakeup() // trådsikker, avbryter konsumer fra polling
            },
        )
    }

    private val sikkerLogg = sikkerlogger()

    abstract fun start()

    protected fun kjørKonsumerLoop(haandter: (ConsumerRecords<K, T>) -> Unit) {
        try {
            logger.info("Starter å lese hendelser fra ${this.javaClass.name}")
            consumer.subscribe(listOf(topic))
            while (!closed.get()) {
                val meldinger: ConsumerRecords<K, T> = consumer.poll(pollTimeoutInSeconds)
                haandter(meldinger)
                consumer.commitSync()
            }
        } catch (e: Exception) {
            if (e is WakeupException && closed.get()) {
                logger.info("Konsument ble bedt om å stenge ned")
            } else {
                logger.error("Uventet feil – avslutter konsumer - [se sikkerlogg for detaljer]")
                sikkerLogg.error("Uventet feil – avslutter konsumer", e)
                throw e
            }
        } finally {
            logger.info("Ferdig med å lese hendelser fra ${this.javaClass.name} - lukker consumer")
            consumer.close()
        }
    }
}
