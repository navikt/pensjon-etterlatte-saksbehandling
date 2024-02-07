package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEP_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class HendelsePublisher(private val rapidsPublisher: (UUID, String) -> Unit) {
    fun publish(
        hendelse: Hendelse,
        jobbType: JobbType,
    ) {
        val message =
            JsonMessage.newMessage(
                mapOf(
                    EventNames.ALDERSOVERGANG.lagParMedEventNameKey(),
                    SAK_ID_KEY to hendelse.sakId,
                    ALDERSOVERGANG_ID_KEY to hendelse.id,
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
