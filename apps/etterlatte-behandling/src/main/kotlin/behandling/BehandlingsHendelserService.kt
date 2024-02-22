package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.BEHANDLING_RIVER_KEY
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface BehandlingHendelserKafkaProducer {
    fun sendMeldingForHendelseMedDetaljertBehandling(
        statistikkBehandling: StatistikkBehandling,
        hendelseType: BehandlingHendelseType,
    )
}

class BehandlingsHendelserKafkaProducerImpl(
    private val rapid: KafkaProdusent<String, String>,
) : BehandlingHendelserKafkaProducer {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendMeldingForHendelseMedDetaljertBehandling(
        statistikkBehandling: StatistikkBehandling,
        hendelseType: BehandlingHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid.publiser(
            statistikkBehandling.id.toString(),
            JsonMessage.newMessage(
                hendelseType.lagEventnameForType(),
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    TEKNISK_TID_KEY to LocalDateTime.now(),
                    BEHANDLING_RIVER_KEY to statistikkBehandling,
                ),
            ).toJson(),
        ).also { (partition, offset) ->
            logger.info(
                "Posted event ${hendelseType.lagEventnameForType()} for behandling ${statistikkBehandling.id}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId",
            )
        }
    }
}
