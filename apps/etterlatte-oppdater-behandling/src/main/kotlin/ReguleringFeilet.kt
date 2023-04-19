package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.sakId

internal class ReguleringFeilet(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(ReguleringFeilet::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(FEILA)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireValue("aarsak", "REGULERING") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Regulering har feilet for sak ${packet.sakId}")
            behandlingService.sendReguleringFeiletHendelse(ReguleringFeiletHendelse(packet.sakId))
        }
}