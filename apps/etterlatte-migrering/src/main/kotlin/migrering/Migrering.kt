package migrering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    private val eventName = "MIGRER"

    init {
        River(rapidsConnection).apply {
            eventName(eventName) // TODO hent frå behandling-greina
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, eventName) {
                val behandlingId = packet.behandlingId

                // ny logikk her

                packet.eventName = EventNames.BEREGN // todo hent frå behandling-pr-kode
                context.publish(packet.toJson())
                logger.info(
                    "Migrering starta for pesys-sak $behandlingId og melding om behandling ble sendt."
                )
            }
        }
}