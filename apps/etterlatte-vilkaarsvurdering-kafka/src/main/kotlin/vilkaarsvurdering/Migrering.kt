package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.VILKAARSVURDER
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : ListenerMedLoggingOgFeilhaandtering(rapidsConnection, VILKAARSVURDER) {

    init {
        initialiser {
            eventName(hendelsestype)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.rejectKey(VILKAARSVURDERT_KEY) }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val behandlingId = packet.behandlingId
        logger.info("Mottatt vilkårs-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")
        vilkaarsvurderingService.migrer(behandlingId)
        packet[VILKAARSVURDERT_KEY] = true
        packet.eventName = Migreringshendelser.TRYGDETID
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert migreringshendelse fra vilkårsvurdering for behandling $behandlingId")
    }
}