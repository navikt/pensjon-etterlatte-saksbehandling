package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.Migreringshendelser
import rapidsandrivers.withFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Vilkaarsvurder::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.VILKAARSVURDER)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.VILKAARSVURDER) {
                logger.info("Mottatt vilkårs-migreringshendelse")
                val behandlingId = packet.behandlingId

                vilkaarsvurderingService.oppdaterTotalVurdering(
                    behandlingId,
                    VurdertVilkaarsvurderingResultatDto(
                        resultat = VilkaarsvurderingUtfall.OPPFYLT,
                        kommentar =
                        "Automatisk overført fra Pesys. Enkeltvilkår ikke vurdert, totalvurdering satt til oppfylt."
                    )
                )

                packet.eventName = Migreringshendelser.BEREGN
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse fra vilkårsvurdering for behandling $behandlingId")
            }
        }
    }
}