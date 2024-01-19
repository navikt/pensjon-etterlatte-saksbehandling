package no.nav.etterlatte.behandling.klage

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface IKlageHendelserService {
    fun sendKlageHendelse(
        statistikkKlage: StatistikkKlage,
        klageHendelseType: KlageHendelseType,
    )
}

class KlageHendelserServiceImpl(
    private val rapid: KafkaProdusent<String, String>,
) : IKlageHendelserService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendKlageHendelse(
        statistikkKlage: StatistikkKlage,
        klageHendelseType: KlageHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid.publiser(
            statistikkKlage.id.toString(),
            JsonMessage.newMessage(
                "KLAGE:${klageHendelseType.name}",
                mapOf(
                    CORRELATION_ID_KEY to correlationId,
                    TEKNISK_TID_KEY to LocalDateTime.now(),
                    STATISTIKK_RIVER_KEY to statistikkKlage,
                ),
            ).toJson(),
        ).also { (partition, offset) ->
            logger.info(
                "Posted event KLAGE:${klageHendelseType.name} for KLAGE ${statistikkKlage.id}" +
                    " to partiton $partition, offset $offset correlationid: $correlationId",
            )
        }
    }
}
