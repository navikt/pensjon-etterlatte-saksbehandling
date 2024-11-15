package no.nav.etterlatte.hendelserufoere.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserufoere.UfoereHendelse
import no.nav.etterlatte.hendelserufoere.UfoereHendelseFordeler
import no.nav.etterlatte.kafka.Kafkakonsument
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class UfoerehendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val ufoereHendelseFordeler: UfoereHendelseFordeler,
) : Kafkakonsument<UfoereHendelse>(
        logger = LoggerFactory.getLogger(KafkaConsumer::class.java.name),
        consumer = KafkaConsumer<String, UfoereHendelse>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser =
                    hendelser.map {
                        async(context = Dispatchers.Default) {
                            ufoereHendelseFordeler.haandterHendelse(it.value())
                        }
                    }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}
