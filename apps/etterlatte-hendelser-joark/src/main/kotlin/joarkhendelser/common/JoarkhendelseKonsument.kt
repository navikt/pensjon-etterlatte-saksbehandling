package no.nav.etterlatte.joarkhendelser.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class JoarkhendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val joarkHendelseHandler: JoarkHendelseHandler,
) : Kafkakonsument<JournalfoeringHendelseRecord>(
        logger = LoggerFactory.getLogger(JoarkhendelseKonsument::class.java.name),
        consumer = KafkaConsumer<String, JournalfoeringHendelseRecord>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser =
                    hendelser.map {
                        async(context = Dispatchers.Default) {
                            joarkHendelseHandler.haandterHendelse(it)
                        }
                    }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}
