package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

interface TilbakekrevingHendelserService {
    fun sendTilbakekreving(
        statistikkTilbakekreving: StatistikkTilbakekrevingDto,
        type: TilbakekrevingHendelseType,
    )
}

class TilbakekrevingHendelserServiceImpl(
    private val rapid: KafkaProdusent<String, String>,
) : TilbakekrevingHendelserService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun sendTilbakekreving(
        statistikkTilbakekreving: StatistikkTilbakekrevingDto,
        type: TilbakekrevingHendelseType,
    ) {
        val correlationId = getCorrelationId()

        rapid
            .publiser(
                statistikkTilbakekreving.id.toString(),
                JsonMessage
                    .newMessage(
                        type.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to LocalDateTime.now(),
                            TILBAKEKREVING_STATISTIKK_RIVER_KEY to statistikkTilbakekreving,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event ${type.lagEventnameForType()} for TILBAKEKREVING ${statistikkTilbakekreving.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }
}
