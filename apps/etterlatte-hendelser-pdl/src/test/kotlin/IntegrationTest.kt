import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.JsonMessage
import no.nav.etterlatte.hendelserpdl.Dodsmeldinger
import no.nav.etterlatte.hendelserpdl.FinnDodsmeldinger
import no.nav.etterlatte.hendelserpdl.MessageProblems
import no.nav.etterlatte.hendelserpdl.TestConfig
import no.nav.etterlatte.hendelserpdl.leesah.LivetErEnStroemAvHendelser
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

@Disabled
class IntegrationTest {
    companion object {
        val kafkaEnv = KafkaEnvironment(
            noOfBrokers = 1,
            topicNames = listOf("etterlatte.dodsmelding", "aapen-person-pdl-leesah-v1"),
            withSecurity = false,
            //users = listOf(JAASCredential("myP1", "myP1p"),JAASCredential("myC1", "myC1p")),
            autoStart = true,
            withSchemaRegistry = true
        )

        @AfterAll
        @JvmStatic
        fun teardown() {
            kafkaEnv.tearDown()
        }
    }

    @Test
    fun test() {

        val leesahTopic: KafkaProducer<String, Personhendelse> = producerForLeesah()
        val rapid: KafkaConsumer<String, String> = consumerForRapid()

        val app = testApp()

        rapid.subscribe(listOf("etterlatte.dodsmelding"))
        app.stream()
        leesahTopic.send(
            ProducerRecord(
                "aapen-person-pdl-leesah-v1",
                Personhendelse(
                    "1",
                    listOf("1234567"),
                    "",
                    Instant.now(),
                    "DOEDSFALL_V1",
                    Endringstype.OPPRETTET,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null

                )
            )
        ).get()

        app.stream()

        rapid.poll(Duration.ofSeconds(4L)).apply {
            assertEquals(1, this.count())
        }.forEach {
            val msg = JsonMessage(it.value(), MessageProblems(it.value()))
            println(it.value())
            msg.interestedIn("@avdod_ident", "@event_name", "system_read_count")
            assertEquals("1234567", msg["@avdod_ident"].textValue())
            assertEquals("person_dod", msg["@event_name"].textValue())
            assertEquals(1, msg["system_read_count"].asInt())
        }

    }

    private fun testApp() = FinnDodsmeldinger(
        LivetErEnStroemAvHendelser(
            mapOf(
                "LEESAH_KAFKA_BROKERS" to kafkaEnv.brokersURL,
                "LEESAH_KAFKA_GROUP_ID" to "leesah-consumer",
                "LEESAH_KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!
            )
        ), Dodsmeldinger(TestConfig(true, mapOf("KAFKA_BROKERS" to kafkaEnv.brokersURL)))
    )


    private fun consumerForRapid() = KafkaConsumer(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ConsumerConfig.GROUP_ID_CONFIG to "test",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "10",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "100"
        ), StringDeserializer(), StringDeserializer()
    )

    private fun producerForLeesah() = KafkaProducer<String, Personhendelse>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            "schema.registry.url" to kafkaEnv.schemaRegistry?.url,
            ProducerConfig.ACKS_CONFIG to "all",
        )
    )
}