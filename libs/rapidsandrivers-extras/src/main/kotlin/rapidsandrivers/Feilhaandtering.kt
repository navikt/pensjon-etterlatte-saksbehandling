package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.common.rapidsandrivers.feilmelding
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val feilhaandteringLogger = LoggerFactory.getLogger("feilhaandtering-kafka")
val sikkerLogg: Logger = sikkerlogger()

internal fun withRetryOgFeilhaandtering(
    packet: JsonMessage,
    kontekst: Kontekst,
    context: MessageContext,
    feilendeSteg: String,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Exception) {
        val antallRetries = kontekst.retries
        val antallKjoeringer = packet[ANTALL_RETRIES_KEY].asInt()
        if (antallKjoeringer == 0 && antallKjoeringer < antallRetries) {
            packet[ANTALL_RETRIES_KEY] = antallKjoeringer + 1
            context.publish(packet.toJson())
        } else {
            feilhaandteringLogger.warn("Håndtering av melding ${packet.id} feila på steg $feilendeSteg.", e)
            try {
                packet.setEventNameForHendelseType(EventNames.FEILA)
                packet.feilendeSteg = feilendeSteg
                packet[KONTEKST_KEY] = kontekst.name
                packet.feilmelding = e.stackTraceToString()
                context.publish(packet.toJson())
                feilhaandteringLogger.info("Publiserte feila-melding")
            } catch (e2: Exception) {
                feilhaandteringLogger.warn("Feil under feilhåndtering for ${packet.id}", e2)
            }
            feilhaandteringLogger.warn("Fikk feil, sendte ut på feilkø, returnerer nå failure-result")
            return
        }
    }
}
