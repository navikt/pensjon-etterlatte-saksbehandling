package no.nav.etterlatte

import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.system.exitProcess

val GRUNNBELOEP_REGULERING_DATO: LocalDate = LocalDate.of(LocalDate.now().year, Month.MAY, 1)

val logger: Logger = LoggerFactory.getLogger("StartReguleringJob")

fun main() {
    logger.info("StartReguleringJob startet")
    val env = System.getenv()
    val topic = env.getValue("KAFKA_TARGET_TOPIC")

    logger.info("Konfig lest, oppretter kafka-produsent")
    val producer = GcpKafkaConfig.fromEnv(env).standardProducer(topic)

    producer.publiser(
        noekkel = "StartReguleringJob-${UUID.randomUUID()}",
        verdi = createRecord(GRUNNBELOEP_REGULERING_DATO),
    )
    producer.close()

    logger.info("StartReguleringJob ferdig")
    exitProcess(0)
}

private fun createRecord(dato: LocalDate) =
    JsonMessage.newMessage(
        mapOf(
            ReguleringHendelseType.START_REGULERING.lagParMedEventNameKey(),
            ReguleringEvents.DATO to dato.toString(),
        ),
    ).toJson()
