package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.REQUEST
import no.nav.etterlatte.rapidsandrivers.migrering.request
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Migrering::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.VILKAARSVURDER)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(REQUEST) }
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.VILKAARSVURDER) {
                logger.info("Mottatt vilkårs-migreringshendelse for $BEHANDLING_ID_KEY ${packet.behandlingId}")
                vilkaarsvurderingService.migrer(tilVilkaarsvurderingMigreringRequest(packet))
                packet.eventName = Migreringshendelser.TRYGDETID
                context.publish(packet.toJson())
                logger.info(
                    "Publiserte oppdatert migreringshendelse fra vilkårsvurdering for behandling ${packet.behandlingId}"
                )
            }
        }
    }

    private fun tilVilkaarsvurderingMigreringRequest(packet: JsonMessage): VilkaarsvurderingMigreringRequest =
        objectMapper.readValue(packet.request, MigreringRequest::class.java)
            .let {
                VilkaarsvurderingMigreringRequest(
                    sakId = packet.sakId,
                    behandlingId = packet.behandlingId,
                    fnr = FoedselsnummerDTO(it.fnr.value),
                    persongalleri = it.persongalleri
                )
            }
}