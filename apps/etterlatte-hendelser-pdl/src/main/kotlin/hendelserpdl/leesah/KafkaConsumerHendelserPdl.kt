package no.nav.etterlatte.hendelserpdl.leesah

import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerHendelserPdl(
    private val personHendelseFordeler: PersonHendelseFordeler,
    env: Map<String, String>,
    private val closed: AtomicBoolean,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) {
    private var antallMeldinger = 0
    private val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private val consumer: KafkaConsumer<String, Personhendelse>
    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env)).also {
            it.subscribe(listOf(leesahtopic))
        }
    }

    fun getConsumer() = consumer

    fun stream() {
        try {
            while (!closed.get()) {
                val meldinger = consumer.poll(Duration.ofSeconds(10L))
                meldinger.forEach {
                    personHendelseFordeler.haandterHendelse(it.value())
                }
                consumer.commitSync()
                antallMeldinger += meldinger.count()
                if (meldinger.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignore exception if closing
            if (!closed.get()) throw e
        } finally {
            consumer.close()
        }
    }
}