package no.nav.etterlatte.hendelserpdl.leesah

import io.ktor.server.application.Application
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
    val logger = LoggerFactory.getLogger(Application::class.java)

    private var antallMeldinger = 0
    private val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private val consumer: KafkaConsumer<String, Personhendelse>
    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env)).also {
            it.subscribe(listOf(leesahtopic))
        }
    }

    fun getAntallMeldinger() = antallMeldinger
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
            // Ignorerer exception hvis vi stenger ned
            if (!closed.get()) throw e
        } finally {
            logger.info("Lukker kafkaconsumer")
            consumer.close()
        }
    }
}