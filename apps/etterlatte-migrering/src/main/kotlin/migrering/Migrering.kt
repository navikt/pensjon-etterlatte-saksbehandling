package migrering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.MigreringRequest
import rapidsandrivers.migrering.Migreringshendelser
import rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import rapidsandrivers.migrering.request
import rapidsandrivers.withFeilhaandtering

internal class Migrering(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(START_MIGRERING)
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, START_MIGRERING) {
                pesysRepository.hentSaker().forEach { migrerSak(packet, it, context) }
            }
        }

    private fun migrerSak(
        packet: JsonMessage,
        it: Pesyssak,
        context: MessageContext
    ) {
        packet.eventName = Migreringshendelser.MIGRER_SAK
        val request = tilMigreringsrequest(it).toJson()
        packet.request = request
        context.publish(packet.toJson())
        logger.info(
            "Migrering starta for pesys-sak ${it.id} og melding om behandling ble sendt."
        )
    }

    private fun tilMigreringsrequest(sak: Pesyssak) = MigreringRequest(
        pesysId = sak.id,
        enhet = sak.enhet,
        fnr = sak.folkeregisteridentifikator,
        mottattDato = sak.mottattdato,
        persongalleri = sak.persongalleri
    )
}