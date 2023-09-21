package rapidsandrivers

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

val feilhaandteringLogger = LoggerFactory.getLogger("feilhaandtering-kafka")

fun <T> withFeilhaandtering(
    packet: JsonMessage,
    context: MessageContext,
    feilendeSteg: String,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        feilhaandteringLogger.error("Håndtering av melding ${packet.id} feila på steg $feilendeSteg.", e)
        packet.eventName = EventNames.FEILA
        packet.feilendeSteg = feilendeSteg
        packet.feilmelding = e
        context.publish(packet.toJson())
        Result.failure(e)
    }
