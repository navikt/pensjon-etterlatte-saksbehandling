package no.nav.etterlatte.hendelserpdl.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecords
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
    private val closed = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                closed.set(true)
                consumer.wakeup(); // trådsikker, aborter konsumer fra polling
            }
        )
    }

    private var antallMeldinger = 0
    private val pollTimeoutSeconds: Duration = Duration.ofSeconds(10L)

    fun konsumer(haandter: suspend (T) -> Unit) {
        val haandterMeldinger: (it: ConsumerRecords<String, T>) -> Unit = {
            runBlocking {
                val ventbareHendelser = it.map {
                    runBlocking {
                        async(context = Dispatchers.Default) {
                            haandter(it.value())
                        }
                    }
                }
                ventbareHendelser.forEach { it.await() }
            }
        }

        try {
            logger.info("Starter å lese hendelser fra ${this.javaClass.name}")
            consumer.subscribe(listOf(topic))
            while (!closed.get()) {
                val meldinger: ConsumerRecords<String, T> = consumer.poll(pollTimeoutSeconds)
                haandterMeldinger(meldinger)
                consumer.commitSync()

                antallMeldinger += meldinger.count()
                if (meldinger.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (closed.get()) throw e
        } finally {
            logger.info("Ferdig med å lese hendelser fra $topic - lukker consumer")
            consumer.close()
        }
    }
}