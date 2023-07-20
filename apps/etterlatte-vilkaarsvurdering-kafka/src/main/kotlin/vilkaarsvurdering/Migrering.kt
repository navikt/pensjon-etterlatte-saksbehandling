package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.VILKAARSVURDER
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(VILKAARSVURDER)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.rejectKey(VILKAARSVURDERT_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, VILKAARSVURDER) {
                val behandlingId = packet.behandlingId
                logger.info("Mottatt vilkårs-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")
                vilkaarsvurderingService.migrer(behandlingId)
                packet[VILKAARSVURDERT_KEY] = true
                packet.eventName = Migreringshendelser.TRYGDETID
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse fra vilkårsvurdering for behandling $behandlingId")
            }
        }
    }
}