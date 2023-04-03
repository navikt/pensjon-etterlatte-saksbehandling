package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.OMREGNINGSHENDELSE
import no.nav.etterlatte.rapidsandrivers.EventNames.VILKAARSVURDER
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.withFeilhaandtering

internal class OmregningsHendelser(rapidsConnection: RapidsConnection, private val behandlinger: BehandlingService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for omregningshendelser")
        River(rapidsConnection).apply {
            eventName(OMREGNINGSHENDELSE)

            correlationId()
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, OMREGNINGSHENDELSE) {
                logger.info("Mottatt omregningshendelse")

                val hendelse: Omregningshendelse = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
                val (behandlingId, behandlingViOmregnerFra) = behandlinger.opprettOmregning(hendelse)
                packet.behandlingId = behandlingId
                packet[BEHANDLING_VI_OMREGNER_FRA_KEY] = behandlingViOmregnerFra
                packet.eventName = VILKAARSVURDER
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert omregningshendelse")
            }
        }
    }
}