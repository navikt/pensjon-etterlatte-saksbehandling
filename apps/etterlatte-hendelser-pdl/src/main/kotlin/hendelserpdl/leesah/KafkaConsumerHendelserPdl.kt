package no.nav.etterlatte.hendelserpdl.leesah

import kotlinx.coroutines.delay
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaConsumerHendelserPdl(
    private val personHendelseFordeler: PersonHendelseFordeler,
    env: Map<String, String>,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) {
    val log: Logger = LoggerFactory.getLogger(KafkaConsumerHendelserPdl::class.java)
    private var antallMeldinger = 0
    private val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private val consumer: KafkaConsumer<String, Personhendelse>
    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env)).also {
            it.subscribe(listOf(leesahtopic))
        }
        Runtime.getRuntime().addShutdownHook(Thread { consumer.close() })
    }

    suspend fun stream(): Int {
        val meldinger = consumer.poll(Duration.ofSeconds(10L))
        meldinger.forEach {
            personHendelseFordeler.haandterHendelse(it.value())
        }
        antallMeldinger += meldinger.count()
        if (meldinger.isEmpty) delay(500)
        return antallMeldinger
    }
}