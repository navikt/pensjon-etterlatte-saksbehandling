package no.nav.etterlatte.hendelserpdl.leesah

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class LeesahConsumer(
    env: Map<String, String>,
    private val topic: String,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) {
    val consumer: KafkaConsumer<String, Personhendelse>

    private val logger = LoggerFactory.getLogger(LeesahConsumer::class.java)

    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env))
    }

    fun lesHendelserFraLeesah(readyToConsume: AtomicBoolean, haandterHendelse: suspend (Personhendelse) -> Unit) {
        try {
            logger.info("Starter å lese hendelser fra Leesah")
            consumer.subscribe(listOf(topic))

            while (readyToConsume.get()) {
                val hendelser = consumer.poll(Duration.ofSeconds(10L))
                runBlocking {
                    val ventbareHendelser = hendelser.map {
                        async(context = Dispatchers.Default) {
                            haandterHendelse(it.value())
                        }
                    }
                    ventbareHendelser.forEach { it.await() }
                }
                consumer.commitSync()

                if (hendelser.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (readyToConsume.get()) throw e
        } finally {
            logger.info("Ferdig med å lese hendelser fra Leesah - lukker consumer")
            consumer.close()
        }
    }
}