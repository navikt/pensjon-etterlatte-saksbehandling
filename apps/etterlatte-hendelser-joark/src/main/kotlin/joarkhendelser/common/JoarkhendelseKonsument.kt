package no.nav.etterlatte.joarkhendelser.common

import joarkhendelser.common.JournalfoeringHendelse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.kafka.Kafkakonsument
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class JoarkhendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val joarkHendelseHandler: JoarkHendelseHandler,
) : Kafkakonsument<JournalfoeringHendelse>(
        logger = LoggerFactory.getLogger(KafkaConsumer::class.java.name),
        consumer = KafkaConsumer<String, JournalfoeringHendelse>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser =
                    hendelser.map {
                        async(context = Dispatchers.Default) {
                            joarkHendelseHandler.haandterHendelse(it.value())
                        }
                    }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}
