package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.KONTEKST_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class ReguleringFeiletRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(ReguleringFeiletRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, FEILA) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireValue(KONTEKST_KEY, Kontekst.REGULERING.name) }
            validate { it.requireKey(KJOERING) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Regulering har feilet for sak ${packet.sakId}")
        behandlingService.lagreKjoering(
            kjoering = packet[KJOERING].asText(),
            sakId = packet.sakId,
            status = KjoeringStatus.FEILA,
        )
    }
}
