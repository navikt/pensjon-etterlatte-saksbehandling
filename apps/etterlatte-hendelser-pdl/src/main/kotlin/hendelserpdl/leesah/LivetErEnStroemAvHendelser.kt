package no.nav.etterlatte.hendelserpdl.leesah

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.time.Duration
import java.util.*

interface ILivetErEnStroemAvHendelser {
    fun poll(consumePersonHendelse: (Personhendelse) -> Unit): Int
    fun fraStart()
}

class LivetErEnStroemAvHendelser(env: Map<String, String>) : ILivetErEnStroemAvHendelser {
    val logger = LoggerFactory.getLogger(LivetErEnStroemAvHendelser::class.java)

    val leesahtopic = "pdl.leesah-v1"
    private var consumer: KafkaConsumer<String, Personhendelse>? = null

    init {

        val startuptask = {
            consumer = KafkaConsumer(
                KafkaConfig(
                    bootstrapServers = env["LEESAH_KAFKA_BROKERS"]!!,
                    consumerGroupId = env["LEESAH_KAFKA_GROUP_ID"]!!,
                    clientId = if (env.containsKey("NAIS_APP_NAME")) {
                        InetAddress.getLocalHost().hostName
                    } else {
                        UUID.randomUUID().toString()
                    },
                    username = env["srvuser"],
                    password = env["srvpwd"],
                    autoCommit = env["KAFKA_AUTO_COMMIT"]?.let { "true" == it.lowercase() },
                    schemaRegistryUrl = env["LEESAH_KAFKA_SCHEMA_REGISTRY"],
                    autoOffsetResetConfig = "latest"
                ).consumerConfig()
            )
            consumer?.subscribe(listOf(leesahtopic))

            logger.info("kafka consumer startet")
            Runtime.getRuntime().addShutdownHook(Thread { consumer?.close() })
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
        val meldinger = consumer?.poll(Duration.ofSeconds(10))
        meldinger?.forEach {
            consumePersonHendelse(it.value())
        }
        consumer?.commitSync()
        return meldinger?.count() ?: 0
    }

    override fun fraStart() {
        consumer?.seekToBeginning(emptyList())
        consumer?.commitSync()
    }
}