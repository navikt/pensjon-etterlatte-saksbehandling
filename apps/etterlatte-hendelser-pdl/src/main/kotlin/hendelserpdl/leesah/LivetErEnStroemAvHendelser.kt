package no.nav.etterlatte.hendelserpdl.leesah

import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

interface ILivetErEnStroemAvHendelser {
    fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int
    fun fraStart()
}

class LivetErEnStroemAvHendelser(
    env: Map<String, String>,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) : ILivetErEnStroemAvHendelser {
    val logger = LoggerFactory.getLogger(LivetErEnStroemAvHendelser::class.java)

    val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private var consumer: KafkaConsumer<String, Personhendelse>
    init {
        consumer = KafkaConsumer<String, Personhendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env))
        consumer.subscribe(listOf(leesahtopic))

        logger.info("kafka consumer startet")
        Runtime.getRuntime().addShutdownHook(Thread { consumer.close() })
    }

    override fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int {
        val meldinger = consumer.poll(Duration.ofSeconds(10L))

        meldinger?.forEach {
            consumePersonHendelse(it.value())
        }

        consumer.commitSync()
        return meldinger?.count() ?: 0
    }

    override fun fraStart() {
        consumer.seekToBeginning(emptyList())
        consumer.commitSync()
    }
}