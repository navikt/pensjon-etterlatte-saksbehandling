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

class KafkaConsumerHendelserPdl(
    private val personHendelseFordeler: PersonHendelseFordeler,
    env: Map<String, String>,
    private val closed: AtomicBoolean,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) {
    val logger = LoggerFactory.getLogger(KafkaConsumerHendelserPdl::class.java)

    private var antallMeldinger = 0
    private val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private val consumer: KafkaConsumer<String, Personhendelse>
    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env))
    }

    fun getAntallMeldinger() = antallMeldinger
    fun getConsumer() = consumer

    fun stream() {
        try {
            logger.info("Starter KafkaConsumerHendelserPdl konsumer")
            consumer.subscribe(listOf(leesahtopic))
            while (!closed.get()) {
                val meldinger = consumer.poll(Duration.ofSeconds(10L))
                runBlocking {
                    val ventbareHendelser = meldinger.map {
                        async(context = Dispatchers.Default) {
                            personHendelseFordeler.haandterHendelse(it.value())
                        }
                    }
                    ventbareHendelser.forEach {
                        it.await()
                    }
                }
                consumer.commitSync()

                antallMeldinger += meldinger.count()
                if (meldinger.isEmpty) Thread.sleep(500L)
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (!closed.get()) throw e
        } finally {
            logger.info("Lukker KafkaConsumerHendelserPdl kafkaconsumer")
            consumer.close()
        }
    }
}