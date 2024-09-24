package no.nav.etterlatte.regulering

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OmregningsHendelserBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val behandlinger: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.UTFORT_SJEKK_AAPEN_OVERSTYRT) {
            validate { it.rejectKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregningshendelse")

        val hendelse: Omregningshendelse = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY])
        val (behandlingId, _, sakType) = behandlinger.opprettOmregning(hendelse)
        packet.behandlingId = behandlingId
        packet[SAK_TYPE] = sakType
        packet.setEventNameForHendelseType(ReguleringHendelseType.BEHANDLING_OPPRETTA)
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert omregningshendelse")
    }
}
