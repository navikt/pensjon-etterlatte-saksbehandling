package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.etterlatte.libs.common.logging.withLogContext
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class SamordningHendelseKonsument(
    topic: String,
    kafkaProperties: Properties,
    private val handler: SamordningHendelseHandler,
) : Kafkakonsument<SamordningVedtakHendelse>(
        logger = LoggerFactory.getLogger(SamordningHendelseKonsument::class.java.name),
        consumer = KafkaConsumer<String, SamordningVedtakHendelse>(kafkaProperties),
        topic = topic,
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { meldinger ->
            meldinger
                .forEach {
                    withLogContext {
                        handler.handleSamordningHendelse(
                            hendelse = it.value(),
                            hendelseKey = it.key(),
                        )
                    }
                }
        }
    }
}
