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

internal fun <T> withFeilhaandtering(
    packet: JsonMessage,
    context: MessageContext,
    feilendeSteg: String,
    kontekst: Kontekst,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        feilhaandteringLogger.error("Håndtering av melding ${packet.id} feila på steg $feilendeSteg.", e)
        try {
            packet.setEventNameForHendelseType(EventNames.FEILA)
            packet.feilendeSteg = feilendeSteg
            packet[KONTEKST_KEY] = kontekst.name
            packet.feilmelding = e.stackTraceToString()
            context.publish(packet.toJson())
            feilhaandteringLogger.info("Publiserte feila-melding")
            val sikkerLogg: Logger = sikkerlogger()
            sikkerLogg.error("Håndtering av melding ${packet.id} feila på steg $feilendeSteg. med body ${packet.toJson()}", e)
        } catch (e2: Exception) {
            feilhaandteringLogger.warn("Feil under feilhåndtering for ${packet.id}", e2)
        }
        feilhaandteringLogger.warn("Fikk feil, sendte ut på feilkø, returnerer nå failure-result")
        Result.failure(e)
    }
