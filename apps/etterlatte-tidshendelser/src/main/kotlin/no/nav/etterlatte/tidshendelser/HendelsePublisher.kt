package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class HendelsePublisher(
    private val rapidsPublisher: (UUID, String) -> Unit,
) {
    fun publish(
        hendelse: Hendelse,
        jobb: HendelserJobb,
    ) {
        val message =
            JsonMessage.newMessage(
                mapOf(
                    EventNames.TIDSHENDELSE.lagParMedEventNameKey(),
                    SAK_ID_KEY to hendelse.sakId,
                    TIDSHENDELSE_ID_KEY to hendelse.id,
                    TIDSHENDELSE_STEG_KEY to hendelse.steg,
                    TIDSHENDELSE_TYPE_KEY to jobb.type.name,
                    DATO_KEY to jobb.behandlingsmaaned.atDay(1).toString(),
                    DRYRUN to jobb.dryrun,
                    CORRELATION_ID to getCorrelationId(),
                ),
            )

        rapidsPublisher(
            hendelse.id,
            message.toJson(),
        )
    }
}
