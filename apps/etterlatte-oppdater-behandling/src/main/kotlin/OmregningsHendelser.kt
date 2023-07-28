package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.OMREGNINGSHENDELSE
import no.nav.etterlatte.rapidsandrivers.EventNames.VILKAARSVURDER
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_TYPE
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class OmregningsHendelser(rapidsConnection: RapidsConnection, private val behandlinger: BehandlingService) :
    ListenerMedLoggingOgFeilhaandtering(rapidsConnection, OMREGNINGSHENDELSE) {

    init {
        initialiser {
            eventName(hendelsestype)

            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        logger.info("Mottatt omregningshendelse")

        val hendelse: Omregningshendelse = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
        val (behandlingId, behandlingViOmregnerFra, sakType) = behandlinger.opprettOmregning(hendelse)
        packet.behandlingId = behandlingId
        packet[BEHANDLING_VI_OMREGNER_FRA_KEY] = behandlingViOmregnerFra
        packet[SAK_TYPE] = sakType
        packet.eventName = VILKAARSVURDER
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert omregningshendelse")
    }
}