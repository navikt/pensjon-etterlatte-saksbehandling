package no.nav.etterlatte.beregningkafka

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.Migreringshendelser
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val beregningService: BeregningService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.BEREGN)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.BEREGN) {
                val behandlingId = packet.behandlingId
                logger.info("Mottatt beregnings-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")

                val beregning = beregningService.beregn(behandlingId)

                packet[BEREGNING_KEY] = beregning
                packet.eventName = Migreringshendelser.VEDTAK
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse fra beregning for behandling $behandlingId")
            }
        }
    }
}