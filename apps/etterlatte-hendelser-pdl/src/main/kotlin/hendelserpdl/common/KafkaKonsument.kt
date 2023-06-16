package no.nav.etterlatte.hendelserpdl.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaKonsument<T>(
    private val topic: String,
    kafkaProperties: Properties
) {
    private val consumer: KafkaConsumer<String, T> = KafkaConsumer<String, T>(kafkaProperties)
    private val ready = AtomicBoolean(true)
    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                ready.set(false)
                consumer.wakeup(); // trådsikker, aborter konsumer fra polling
            }
        )
    }

    fun konsumer(haandter: suspend (T) -> Unit) {
        try {
            logger.info("Starter å lese hendelser fra $topic")
            consumer.subscribe(listOf(topic))

            while (ready.get()) {
                val hendelser = consumer.poll(Duration.ofSeconds(10L))
                runBlocking {
                    val ventbareHendelser = hendelser.map {
                        async(context = Dispatchers.Default) {
                            haandter(it.value())
                        }
                    }
                    ventbareHendelser.forEach { it.await() }
                }
                consumer.commitSync()

                if (hendelser.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (ready.get()) throw e
        } finally {
            logger.info("Ferdig med å lese hendelser fra $topic - lukker consumer")
            consumer.close()
        }
    }
}