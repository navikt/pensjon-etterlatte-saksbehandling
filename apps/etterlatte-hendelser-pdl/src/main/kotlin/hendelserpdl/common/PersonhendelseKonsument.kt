package no.nav.etterlatte.hendelserpdl.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.PersonHendelseFordeler
import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class PersonhendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val personHendelseFordeler: PersonHendelseFordeler,
) : Kafkakonsument<Personhendelse>(
        logger = LoggerFactory.getLogger(KafkaConsumer::class.java.name),
        consumer = KafkaConsumer<String, Personhendelse>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { hendelser ->
            runBlocking {
                val ventbareHendelser =
                    hendelser.map {
                        async(context = Dispatchers.Default) {
                            personHendelseFordeler.haandterHendelse(it.value())
                        }
                    }
                ventbareHendelser.forEach { it.await() }
            }
        }
    }
}
