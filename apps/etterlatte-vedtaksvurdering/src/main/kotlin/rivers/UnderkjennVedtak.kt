package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class UnderkjennVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(UnderkjennVedtak::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("SAKSBEHANDLER:UNDERKJENN_VEDTAK")
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("saksbehandler") }
            validate { it.requireKey("kommentar") }
            validate { it.requireKey("valgtBegrunnelse") }
            correlationId()
            validate { it.rejectKey("feil") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandlingId = packet["behandlingId"].asUUID()
            try {
                val vedtak = vedtaksvurderingService.underkjennVedtak(behandlingId)
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            eventNameKey to "VEDTAK:UNDERKJENT",
                            "eventtimestamp" to Tidspunkt.now(),
                            "vedtakId" to vedtak.id,
                            "sakId" to vedtak.sakId.toLong()
                        ) + packet.keep(
                            "behandlingId",
                            "saksbehandler",
                            correlationIdKey,
                            "valgtBegrunnelse",
                            "kommentar"
                        )
                    ).toJson()
                )
            } catch (e: Exception) {
                // TODO endre denne
                logger.warn("spiser en melding fordi ${e.message}", e)
            }
        }
}