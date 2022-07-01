package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class HendelserOmVedtak(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    private val vedtakhendelser = listOf(
        "VEDTAK:FATTET", "VEDTAK:ATTESTERT", "VEDTAK:UNDERKJENT", "VEDTAK:AVKORTET",
        "VEDTAK:BEREGNET", "VEDTAK:VILKAARSVURDERT"
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny("@event", vedtakhendelser) }
            validate { it.requireKey("@behandlingId") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandling = UUID.fromString(packet["@behandlingId"].textValue())
            val hendelse = packet["@event"].textValue()

            logger.info("""Oppdaterer behandling $behandling med hendelse  $hendelse""")
            behandlinger.vedtakHendelse(
                UUID.fromString(packet["@behandlingId"].textValue()),
                hendelse.split(":").last()
            )
        }
}


private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()