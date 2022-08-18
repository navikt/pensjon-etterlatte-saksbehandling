package rapidsandrivers.vedlikehold

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class Vedlikeholdsriver(
    rapidsConnection: RapidsConnection,
    val service: VedlikeholdService
) : River.PacketListener {
    companion object {
        val slettSakEventName = "VEDLIKEHOLD:SLETT_SAK"
    }

    init {
        River(rapidsConnection).apply {
            eventName(slettSakEventName)
            validate { it.requireKey("sakId") }
            validate { it.rejectKey("@feil") }
            validate { it.rejectKey("@resultat") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            service.slettSak(packet["sakId"].asLong())
            packet["@resultat"] = "Sak slettet"
        } catch (ex: Exception) {
            packet["@feil"] = "Feilet med melding: ${ex.message}"
        }
        context.publish(packet.toJson())
    }
}

fun RapidsConnection.registrerVedlikeholdsriver(service: VedlikeholdService) {
    Vedlikeholdsriver(this, service)
}