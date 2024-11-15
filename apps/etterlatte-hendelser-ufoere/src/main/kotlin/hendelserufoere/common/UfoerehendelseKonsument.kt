package no.nav.etterlatte.hendelserufoere.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserufoere.UfoereHendelse
import no.nav.etterlatte.hendelserufoere.UfoereHendelseFordeler
import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.etterlatte.libs.common.deserialize
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class UfoerehendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val ufoereHendelseFordeler: UfoereHendelseFordeler,
) : Kafkakonsument<String>(
        logger = LoggerFactory.getLogger(KafkaConsumer::class.java.name),
        consumer = KafkaConsumer<String, String>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser =
                    hendelser.map {
                        async(context = Dispatchers.Default) {
                            val ufoereHendelse: UfoereHendelse = deserialize(it.value())
                            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
                        }
                    }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}
