package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

abstract class ListenerMedLogging(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    fun initialiser(spesifikkInitialisering: River.() -> Unit) {
        River(rapidsConnection).apply {
            correlationId()
            spesifikkInitialisering()
        }.register(this)
    }

    abstract fun haandterPakke(packet: JsonMessage, context: MessageContext): Any
    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            haandterPakke(packet, context)
        }
}