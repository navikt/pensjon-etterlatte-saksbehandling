package rapidsandrivers

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

val feilhaandteringLogger = LoggerFactory.getLogger("feilhaandtering-kafka")

fun withFeilhaandtering(packet: JsonMessage, context: MessageContext, feilendeSteg: String, block: () -> Unit): Status =
    if (innerFeilhaandtering(packet, context, feilendeSteg, block) != null) {
        Status.SUKSESS
    } else {
        Status.FEIL
    }

private fun <T> innerFeilhaandtering(
    packet: JsonMessage,
    context: MessageContext,
    feilendeSteg: String,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        feilhaandteringLogger.warn("Håndtering av melding ${packet.id} feila på steg $feilendeSteg.", e)
        packet.eventName = EventNames.FEILA
        packet.feilendeSteg = feilendeSteg
        context.publish(packet.toJson())
        null
    }
}

enum class Status {
    SUKSESS,
    FEIL
}