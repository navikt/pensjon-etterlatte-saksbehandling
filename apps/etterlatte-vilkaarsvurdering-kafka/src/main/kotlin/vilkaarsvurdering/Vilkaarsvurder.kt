package no.nav.etterlatte.vilkaarsvurdering

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
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class Vilkaarsvurder(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) :
    ListenerMedLoggingOgFeilhaandtering(rapidsConnection, VILKAARSVURDER) {
    private val logger = LoggerFactory.getLogger(Vilkaarsvurder::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()

        vilkaarsvurderingService.kopierForrigeVilkaarsvurdering(behandlingId, behandlingViOmregnerFra)
        packet.eventName = EventNames.BEREGN
        context.publish(packet.toJson())
        logger.info(
            "Vilkaarsvurdert ferdig for behandling $behandlingId og melding beregningsmelding ble sendt."
        )
    }
}