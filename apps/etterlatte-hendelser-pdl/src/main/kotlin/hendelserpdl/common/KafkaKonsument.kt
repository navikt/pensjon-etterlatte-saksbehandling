package no.nav.etterlatte.hendelserpdl.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kafka.Kafkakonsument
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaKonsument<T>(
    topic: String,
    kafkaProperties: Properties,
    private val haandter: suspend (T) -> Unit
) : Kafkakonsument<T>(
    logger = LoggerFactory.getLogger(KafkaConsumer::class.java.name),
    consumer = KafkaConsumer<String, T>(kafkaProperties),
    topic = topic,
    pollTimeoutInSeconds = Duration.ofSeconds(10L),
    closed = AtomicBoolean(false)
) {

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                closed.set(true)
                consumer.wakeup(); // trådsikker, aborter konsumer fra polling
            }
        )
    }

    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser = hendelser.map {
                    async(context = Dispatchers.Default) {
                        haandter(it.value())
                    }
                }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}