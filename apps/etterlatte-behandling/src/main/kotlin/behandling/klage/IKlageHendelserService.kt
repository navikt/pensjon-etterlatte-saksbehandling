package no.nav.etterlatte.behandling.klage

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.klage.KLAGE_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface IKlageHendelserService {
    fun sendKlageHendelseRapids(
        statistikkKlage: StatistikkKlage,
        klageHendelseType: KlageHendelseType,
    )
}

class KlageHendelserServiceImpl(
    private val rapid: KafkaProdusent<String, String>,
) : IKlageHendelserService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendKlageHendelseRapids(
        statistikkKlage: StatistikkKlage,
        klageHendelseType: KlageHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid
            .publiser(
                statistikkKlage.id.toString(),
                JsonMessage
                    .newMessage(
                        klageHendelseType.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to LocalDateTime.now(),
                            KLAGE_STATISTIKK_RIVER_KEY to statistikkKlage,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${klageHendelseType.lagEventnameForType()} for KLAGE ${statistikkKlage.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }
}
