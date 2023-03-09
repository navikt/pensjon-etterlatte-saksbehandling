package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.EventNames.VILKAARSVURDER
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class Vilkaarsvurder(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(Vilkaarsvurder::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(VILKAARSVURDER)
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, VILKAARSVURDER) {
                val sakId = packet.sakId
                logger.info("Prøver å vilkårsvurdere for sak $sakId")

                val behandlingId = packet.behandlingId
                val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()

                vilkaarsvurderingService.kopierForrigeVilkaarsvurdering(behandlingId, behandlingViOmregnerFra)
                packet.eventName = EventNames.BEREGN
                context.publish(packet.toJson())
                logger.info("Vilkaarsvurdert ferdig for sak $sakId og melding x ble sendt.")
            }
        }
}