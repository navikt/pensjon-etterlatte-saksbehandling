package rapidsandrivers

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

val feilhaandteringLogger = LoggerFactory.getLogger("feilhaandtering-kafka")

fun withFeilhaandtering(packet: JsonMessage, context: MessageContext, block: () -> Unit): Unit =
    innerFeilhaandtering(packet, context, block)

private fun <T> innerFeilhaandtering(packet: JsonMessage, context: MessageContext, block: () -> T): T {
    try {
        return block()
    } catch (e: Exception) {
        feilhaandteringLogger.warn("Håndtering av melding ${packet.id} feila.")
        packet.eventName = EventNames.FEILA
        context.publish(packet.toJson())
        throw e
    }
}