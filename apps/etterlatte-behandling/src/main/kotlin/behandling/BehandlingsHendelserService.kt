package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

enum class BehandlingHendelseType {
    OPPRETTET,
    AVBRUTT,
}

interface BehandlingHendelserKafkaProducer {
    fun sendMeldingForHendelse(
        behandling: Behandling,
        hendelseType: BehandlingHendelseType,
    )
}

class BehandlingsHendelserKafkaProducerImpl(
    private val rapid: KafkaProdusent<String, String>,
) : BehandlingHendelserKafkaProducer {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMeldingForHendelse(
        behandling: Behandling,
        hendelseType: BehandlingHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid.publiser(
            behandling.id.toString(),
            JsonMessage.newMessage(
                "BEHANDLING:${hendelseType.name}",
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    TEKNISK_TID_KEY to LocalDateTime.now(),
                    BehandlingRiverKey.behandlingObjectKey to behandling,
                ),
            ).toJson(),
        ).also { (partition, offset) ->
            logger.info(
                "Posted event BEHANDLING:${hendelseType.name} for behandling ${behandling.id}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId",
            )
        }
    }
}
