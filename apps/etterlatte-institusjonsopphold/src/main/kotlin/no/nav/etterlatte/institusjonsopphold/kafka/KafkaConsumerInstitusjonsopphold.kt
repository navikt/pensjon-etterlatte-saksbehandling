package no.nav.etterlatte.institusjonsopphold.kafka

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.institusjonsopphold.kafka.InstitusjonsoppholdKey.INSTITUSJONSOPPHOLD_TOPIC
import no.nav.etterlatte.institusjonsopphold.klienter.BehandlingKlient
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdsType
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.Kafkakonsument
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

class KafkaConsumerInstitusjonsopphold(
    env: Miljoevariabler,
    private val behandlingKlient: BehandlingKlient,
    kafkaEnvironment: KafkaConsumerConfiguration = KafkaEnvironment(),
) : Kafkakonsument<Long, KafkaOppholdHendelse>(
        logger = LoggerFactory.getLogger(KafkaConsumerInstitusjonsopphold::class.java.name),
        consumer = KafkaConsumer<Long, KafkaOppholdHendelse>(properties(kafkaEnvironment, env)),
        topic = env.requireEnvValue(INSTITUSJONSOPPHOLD_TOPIC),
        pollTimeoutInSeconds = Duration.ofSeconds(10L),
    ) {
    val sikkerLogg: Logger = sikkerlogger()

    override fun stream() {
        stream { meldinger ->
            meldinger.forEach {
                runBlocking {
                    try {
                        behandlingKlient.haandterHendelse(it)
                    } catch (e: RuntimeException) {
                        sikkerLogg.error("Feil i institusjonshendelse: ", e)
                        throw e
                    }
                }
            }
        }
    }
}

internal fun properties(
    kafkaEnvironment: KafkaConsumerConfiguration,
    env: Miljoevariabler,
): Properties =
    kafkaEnvironment.generateKafkaConsumerProperties(env).apply {
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
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
