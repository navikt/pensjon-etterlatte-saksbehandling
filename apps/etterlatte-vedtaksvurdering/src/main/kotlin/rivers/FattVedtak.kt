package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FattVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(FattVedtak::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "SAKSBEHANDLER:FATT_VEDTAK") }
            validate { it.requireKey("@sakId") }
            validate { it.requireKey("@behandlingId") }
            validate { it.requireKey("@vedtakId") }
            validate { it.requireKey("@saksbehandler") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["@behandlingId"].asUUID()
            val sakId = packet["@sakId"].longValue()
            val saksbehandler = packet["@saksbehandler"].textValue()
            val fattetVedtak = vedtaksvurderingService.fattVedtak(sakId.toString(), behandlingId, saksbehandler)
            context.publish(JsonMessage.newMessage(
                mapOf(
                    "@event" to "VEDTAK:FATTET",
                    "@vedtak" to fattetVedtak
                )
            ).toJson())
        }
}