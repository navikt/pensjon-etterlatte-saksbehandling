package no.nav.etterlatte.batch

import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.aremark_person
import no.nav.etterlatte.sendMelding
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndToEndTest {
    private val topicname: String = "test_topic"
    private val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        noOfBrokers = 1,
        topicInfos = listOf(KafkaEnvironment.TopicInfo(name = topicname, partitions = 1)),
        withSchemaRegistry = false,
        withSecurity = false,
        brokerConfigOverrides = Properties().apply {
            this["auto.leader.rebalance.enable"] = "false"
            this["group.initial.rebalance.delay.ms"] =
                "1" //Avoid waiting for new consumers to join group before first rebalancing (default 3000ms)
        }
    )

    @BeforeAll
    internal fun setupAll() {
        embeddedKafkaEnvironment.start()
    }
    @Test
    fun `sender meldinger på rapid`() {
        val producer = KafkaProducer(
            mapOf(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to embeddedKafkaEnvironment.brokersURL.substringAfterLast("/"),
                ProducerConfig.ACKS_CONFIG to "1",
                ProducerConfig.CLIENT_ID_CONFIG to "etterlatte-post-til-kafka",
                ProducerConfig.LINGER_MS_CONFIG to "0",
                ProducerConfig.RETRIES_CONFIG to 5.toString(),
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "1"
            ).toProperties(), StringSerializer(), StringSerializer()
        )


        sendMelding(payload(aremark_person),producer, topicname)

        val consumer = KafkaConsumer(
            mapOf(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to embeddedKafkaEnvironment.brokersURL.substringAfterLast("/"),
                ConsumerConfig.GROUP_ID_CONFIG to "aaa",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",

                ).toProperties(), StringDeserializer(), StringDeserializer()
        )
        consumer.subscribe(listOf(topicname))
        consumer.poll(Duration.ofSeconds(2)).also {
            Assertions.assertEquals(1, it.count())
        }.forEach{
            Assertions.assertEquals("0", it.key())
            Assertions.assertTrue(it.value().contains(""""type":"GJENLEVENDEPENSJON""""))
        }
    }

    @AfterAll
    fun tearDown() {
        embeddedKafkaEnvironment.tearDown()
    }

}