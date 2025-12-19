package no.nav.etterlatte.institusjonsopphold.kafka

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdsType
import no.nav.etterlatte.institusjonsopphold.kafka.InstitusjonsoppholdKey.INSTITUSJONSOPPHOLD_TOPIC
import no.nav.etterlatte.institusjonsopphold.klienter.BehandlingKlient
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaConsumerInstitusjonsopphold(
    env: Miljoevariabler,
    private val behandlingKlient: BehandlingKlient,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment(),
) : Kafkakonsument<KafkaOppholdHendelse>(
        logger = LoggerFactory.getLogger(KafkaConsumerInstitusjonsopphold::class.java.name),
        consumer = KafkaConsumer<String, KafkaOppholdHendelse>(kafkaEnvironment.generateKafkaConsumerProperties(env)),
        topic = env.requireEnvValue(INSTITUSJONSOPPHOLD_TOPIC),
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    override fun stream() {
        stream { meldinger ->
            meldinger.forEach {
                runBlocking {
                    behandlingKlient.haandterHendelse(it)
                }
            }
        }
    }
}

data class KafkaOppholdHendelse(
    val hendelseId: Long,
    var oppholdId: Long,
    var norskident: String,
    var type: InstitusjonsoppholdsType,
    var kilde: InstitusjonsoppholdKilde,
)

enum class InstitusjonsoppholdKey : EnvEnum {
    INSTITUSJONSOPPHOLD_GROUP_ID,
    INSTITUSJONSOPPHOLD_TOPIC,
    ;

    override fun key() = name
}
