package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
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
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.SAK_TYPE
import rapidsandrivers.behandlingId
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
            validate { it.requireKey(SAK_TYPE) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, VILKAARSVURDER) {
                val behandlingId = packet.behandlingId
                val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()
                val sakType = objectMapper.treeToValue<SakType>(packet[SAK_TYPE])

                vilkaarsvurderingService.kopierForrigeVilkaarsvurdering(behandlingId, behandlingViOmregnerFra)
                if (sakType == SakType.BARNEPENSJON) {
                    packet.eventName = EventNames.BEREGN
                } else {
                    packet.eventName = EventNames.TRYGDETID
                }
                context.publish(packet.toJson())
                logger.info(
                    "Vilkaarsvurdert ferdig for behandling $behandlingId og melding beregningsmelding ble sendt."
                )
            }
        }
}