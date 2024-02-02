package no.nav.etterlatte

import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.EventNames
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.system.exitProcess

val logger: Logger = LoggerFactory.getLogger("StartGrunnlagsversjoneringJobb")

fun main() {
    logger.info("StartGrunnlagsversjoneringJobb startet")
    val env = System.getenv()
    val topic = env.getValue("KAFKA_TARGET_TOPIC")

    logger.info("Konfig lest, oppretter kafka-produsent")
    val producer = GcpKafkaConfig.fromEnv(env).standardProducer(topic)

    producer.publiser(
        noekkel = "StartGrunnlagsversjoneringJobb-${UUID.randomUUID()}",
        verdi =
            JsonMessage.newMessage(
                mapOf(EventNames.GRUNNLAGSVERSJONERING_EVENT_NAME.lagParMedEventNameKey()),
            ).toJson(),
    )
    producer.close()

    logger.info("StartGrunnlagsversjoneringJobb ferdig")
    exitProcess(0)
}
