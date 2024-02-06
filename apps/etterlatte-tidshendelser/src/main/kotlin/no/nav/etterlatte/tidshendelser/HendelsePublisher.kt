package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.ALDERSOVERGANG_STEP_KEY
import rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import rapidsandrivers.SAK_ID_KEY
import java.util.UUID

class HendelsePublisher(private val rapidsPublisher: (UUID, String) -> Unit) {
    fun publish(
        hendelse: Hendelse,
        jobbType: JobbType,
    ) {
        val message =
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to EventNames.ALDERSOVERGANG.lagEventnameForType(),
                    "@hendelse_id" to hendelse.id,
                    SAK_ID_KEY to hendelse.sakId,
                    ALDERSOVERGANG_STEP_KEY to hendelse.steg,
                    ALDERSOVERGANG_TYPE_KEY to jobbType.name,
                    CORRELATION_ID to getCorrelationId(),
                ),
            )

        rapidsPublisher(
            hendelse.id,
            message.toJson(),
        )
    }
}
