package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.ReguleringFeiletHendelse
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.ListenerMedLogging
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.sakId

internal class ReguleringFeiletRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(ReguleringFeiletRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, FEILA) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireValue(ReguleringEvents.AARSAK, ReguleringEvents.EVENT_NAME) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Regulering har feilet for sak ${packet.sakId}")
        behandlingService.sendReguleringFeiletHendelse(ReguleringFeiletHendelse(packet.sakId))
    }
}
