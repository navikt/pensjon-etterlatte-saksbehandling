package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory

internal class KopierTrygdetidRiver(
    rapidsConnection: RapidsConnection,
    private val trygdetidService: TrygdetidService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.VILKAARSVURDERT) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregninghendelse, kopierer trygdetid")
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()
        trygdetidService.kopierTrygdetidFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
        packet.setEventNameForHendelseType(OmregningHendelseType.TRYGDETID_KOPIERT)
        context.publish(packet.toJson())
        logger.info("Trygdetid kopiert. Publiserte oppdatert omregningshendelse")
    }
}
