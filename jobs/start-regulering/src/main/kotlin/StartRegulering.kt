package no.nav.etterlatte

import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.standardProducer
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.system.exitProcess

object StartRegulering {
    /** 1e mai 2023 */
    private val GRUNNBELOEP_REGULERING_DATO: LocalDate = LocalDate.of(2023, 5, 1)

    private val logger: Logger = LoggerFactory.getLogger("StartReguleringJob")

    fun start() {
        logger.info("StartReguleringJob startet")
        val env = System.getenv()
        val topic = env.getValue("KAFKA_TARGET_TOPIC")

        logger.info("Konfig lest, oppretter kafka-produsent")
        val producer = GcpKafkaConfig.fromEnv(env).standardProducer(topic)

        producer.publiser(
            noekkel = "StartReguleringJob-${UUID.randomUUID()}",
            verdi = createRecord(GRUNNBELOEP_REGULERING_DATO)
        )
        producer.close()

        logger.info("StartReguleringJob ferdig")
        exitProcess(0)
    }

    private fun createRecord(dato: LocalDate) = JsonMessage.newMessage(
        mapOf("@event_name" to "REGULERING", "dato" to dato.toString())
    ).toJson()
}