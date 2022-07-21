package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class OppdaterBehandling(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("GRUNNLAG:GRUNNLAGENDRET")
            correlationId()
            validate { it.requireKey("sakId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            logger.info("Oppdaterer behandling med at grunnlag er endret i sak ${packet["sakId"].longValue()}")
            behandlinger.grunnlagEndretISak(packet["sakId"].longValue())
        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()