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
import rapidsandrivers.Status
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.Migreringshendelser
import rapidsandrivers.withFeilhaandtering
import java.util.*

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

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
                val behandlingId = packet.behandlingId
                logger.info("Mottatt vilkårs-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")

                oppdaterTotalVurdering(behandlingId)
            }.takeIf { it == Status.SUKSESS }
                ?.let {
                    withFeilhaandtering(packet, context, Migreringshendelser.VILKAARSVURDER) {
                        val behandlingId = packet.behandlingId
                        vilkaarsvurderingService.endreStatusTilIkkeVurdertForAlleVilkaar(behandlingId)
                        sendVidere(packet, context, behandlingId)
                    }
                }
        }
    }

    private fun sendVidere(
        packet: JsonMessage,
        context: MessageContext,
        behandlingId: UUID
    ) {
        packet.eventName = Migreringshendelser.BEREGN
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert migreringshendelse fra vilkårsvurdering for behandling $behandlingId")
    }

    private fun oppdaterTotalVurdering(behandlingId: UUID) = vilkaarsvurderingService.oppdaterTotalVurdering(
        behandlingId,
        VurdertVilkaarsvurderingResultatDto(
            resultat = VilkaarsvurderingUtfall.OPPFYLT,
            kommentar =
            "Automatisk overført fra Pesys. Enkeltvilkår ikke vurdert, totalvurdering satt til oppfylt."
        )
    )
}