package no.nav.etterlatte.kafka

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdsType
import no.nav.etterlatte.libs.common.requireEnvValue
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaConsumerInstitusjonsopphold(
    env: Map<String, String>,
    private val behandlingKlient: BehandlingKlient,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment()
) : Kafkakonsument<KafkaOppholdHendelse>(
    logger = LoggerFactory.getLogger(KafkaConsumerInstitusjonsopphold::class.java.name),
    consumer = KafkaConsumer<String, KafkaOppholdHendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env)),
    topic = env.requireEnvValue("INSTITUSJONSOPPHOLD_TOPIC"),
    pollTimeoutInSeconds = Duration.ofSeconds(10L)
) {
    override fun stream() {
        stream { meldinger -> meldinger.forEach { behandlingKlient.haandterHendelse(it) } }
    }
}

data class KafkaOppholdHendelse(
    val hendelseId: Long,
    var oppholdId: Long,
    var norskident: String,
    var type: InstitusjonsoppholdsType,
    var kilde: InstitusjonsoppholdKilde
)