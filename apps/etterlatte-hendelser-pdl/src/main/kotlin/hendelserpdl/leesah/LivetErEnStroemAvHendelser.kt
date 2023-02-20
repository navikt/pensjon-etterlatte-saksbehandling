package no.nav.etterlatte.hendelserpdl.leesah

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface ILivetErEnStroemAvHendelser {
    fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int
    fun fraStart()
}

@OptIn(DelicateCoroutinesApi::class)
class LivetErEnStroemAvHendelser(env: Map<String, String>) : ILivetErEnStroemAvHendelser {
    val logger = LoggerFactory.getLogger(LivetErEnStroemAvHendelser::class.java)

    val leesahtopic = env["LEESAH_TOPIC_PERSON"]
    private lateinit var consumer: KafkaConsumer<String, Personhendelse>

    init {
        val startuptask = {
            consumer = KafkaConsumer<String, Personhendelse>(generateKafkaConsumerProperties(env))
            consumer.subscribe(listOf(leesahtopic))

            logger.info("kafka consumer startet")
            Runtime.getRuntime().addShutdownHook(Thread { consumer.close() })
        }

        if (env["DELAYED_START"] == "true") {
            GlobalScope.launch {
                logger.info("venter 30s for sidecars")
                delay(30L * 1000L)
                logger.info("starter kafka consumer")
                startuptask()
            }
        } else {
            startuptask()
        }
    }

    override fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int {
        val meldinger = consumer.poll(Duration.ofMinutes(4L))

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