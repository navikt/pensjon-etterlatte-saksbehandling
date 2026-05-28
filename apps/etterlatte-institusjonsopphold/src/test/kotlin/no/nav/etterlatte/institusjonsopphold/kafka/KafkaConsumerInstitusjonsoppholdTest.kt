package no.nav.etterlatte.institusjonsopphold.kafka

import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.libs.common.Miljoevariabler
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Properties

class KafkaConsumerInstitusjonsoppholdTest {
    @Test
    fun `properties overstyrer MAX_POLL_RECORDS_CONFIG til 1`() {
        val stubConfig =
            object : KafkaConsumerConfiguration {
                override fun generateKafkaConsumerProperties(env: Miljoevariabler) =
                    Properties().apply {
                        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                    }
            }
        val env = Miljoevariabler.httpClient(emptyMap())

        val props = lagInstitusjonsoppholdProperties(stubConfig, env)

        assertEquals(1, props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG])
    }
}
