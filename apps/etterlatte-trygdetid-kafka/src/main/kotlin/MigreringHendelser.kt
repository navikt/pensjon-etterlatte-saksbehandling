package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val trygdetidService: TrygdetidService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.TRYGDETID)

            correlationId()
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey("vilkaarsvurdert") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.TRYGDETID) {
                val behandlingId = packet.behandlingId
                logger.info("Mottatt trygdetid-migreringshendelse for behandling $behandlingId")

                val trygdetid = trygdetidService.beregnTrygdetid(behandlingId).beregnetTrygdetid
                    ?: throw IllegalStateException("Trygdetid er udefinert for behandling $behandlingId")

                packet.eventName = Migreringshendelser.BEREGN
                packet["trygdetid"] = trygdetid.toJson()
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse fra trygdetid for behandling $behandlingId")
            }
        }
    }
}