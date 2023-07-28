package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.withFeilhaandtering

abstract class ListenerMedLoggingOgFeilhaandtering(
    rapidsConnection: RapidsConnection,
    protected val hendelsestype: String
) : ListenerMedLogging(rapidsConnection) {

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, hendelsestype) {
                haandterPakke(packet, context)
            }
        }
}