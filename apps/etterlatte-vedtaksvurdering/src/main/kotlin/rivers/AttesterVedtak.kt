package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class AttesterVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(AttesterVedtak::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "SAKSBEHANDLER:ATTESTER_VEDTAK") }
            validate { it.requireKey("@sakId") }
            validate { it.requireKey("@behandlingId") }
            validate { it.requireKey("@vedtakId") }
            validate { it.requireKey("@saksbehandler") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = UUID.fromString(packet["@behandlingId"].textValue())
            val sakId = packet["@sakId"].longValue()
            val saksbehandler = packet["@saksbehandler"].textValue()
            val attestertVedtak = vedtaksvurderingService.attesterVedtak(sakId.toString(), behandlingId, saksbehandler)
            context.publish(JsonMessage.newMessage(
                mapOf(
                    "@event" to "VEDTAK:ATTESTERT",
                    "@vedtak" to attestertVedtak
                )
            ).toJson())
        }
}
private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()