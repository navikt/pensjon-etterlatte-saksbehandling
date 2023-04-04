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
import rapidsandrivers.pesysSakId
import rapidsandrivers.withFeilhaandtering

internal class Migrering(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    private val eventName = "MIGRER"

    init {
        River(rapidsConnection).apply {
            eventName(eventName) // TODO hent frå behandling-greina
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, eventName) {
                pesysRepository.hentSaker().forEach { migrerSak(packet, it, context) }
            }
        }

    private fun migrerSak(
        packet: JsonMessage,
        it: Pesyssak,
        context: MessageContext
    ) {
        packet.eventName = EventNames.BEREGN // todo hent frå behandling-pr-kode
        packet.pesysSakId = it.id
        // todo: Lag MigreringsRequest-objektet her
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${it.id} og melding om behandling ble sendt."
        )
    }
}