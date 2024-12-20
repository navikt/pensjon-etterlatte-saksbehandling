package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.VEDTAK_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

interface TilbakekrevingHendelserService {
    fun sendTilbakekreving(
        statistikkTilbakekreving: StatistikkTilbakekrevingDto,
        type: TilbakekrevingHendelseType,
    )

    fun sendVedtakForJournalfoeringOgDistribusjonAvBrev(
        tilbakekrevingId: UUID,
        vedtak: VedtakDto,
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
                    "Posted event ${type.lagEventnameForType()} for tilbakekreving ${statistikkTilbakekreving.id}" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }

    override fun sendVedtakForJournalfoeringOgDistribusjonAvBrev(
        tilbakekrevingId: UUID,
        vedtak: VedtakDto,
    ) {
        val correlationId = getCorrelationId()
        val type = VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType()

        rapid
            .publiser(
                vedtak.behandlingId.toString(),
                JsonMessage
                    .newMessage(
                        type,
                        mapOf(
                            CORRELATION_ID_KEY to getCorrelationId(),
                            TEKNISK_TID_KEY to LocalDateTime.now(),
                            VEDTAK_KEY to vedtak.toObjectNode(),
                            SKAL_SENDE_BREV to true,
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Posted event $type for tilbakekreving $tilbakekrevingId" +
                        " to partiton $partition, offset $offset correlationid: $correlationId",
                )
            }
    }
}
