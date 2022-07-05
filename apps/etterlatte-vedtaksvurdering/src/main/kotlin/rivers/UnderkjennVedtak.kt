package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
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
            validate { it.demandValue("@event", "SAKSBEHANDLER:UNDERKJENN_VEDTAK") }
            validate { it.requireKey("@sakId") }
            validate { it.requireKey("@behandlingId") }
            validate { it.requireKey("@vedtakId") }
            validate { it.requireKey("@saksbehandler") }
            validate { it.requireKey("@kommentar") }
            validate { it.requireKey("@valgtBegrunnelse") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["@behandlingId"].asUUID()
            val sakId = packet["@sakId"].longValue()
            val saksbehandler = packet["@saksbehandler"].textValue()
           try {
               vedtaksvurderingService.underkjennVedtak(
                   sakId.toString(),
                   behandlingId,
                   saksbehandler,
                   packet["@kommentar"].textValue(),
                   packet["@valgtBegrunnelse"].textValue()
               )
               context.publish(
                   JsonMessage.newMessage(
                       mapOf(
                           "@event" to "VEDTAK:UNDERKJENT",
                           "@eventtimestamp" to Tidspunkt.now(),
                       ) + packet.keep(
                           "@vedtakId",
                           "@behandlingId",
                           "@saksbehandler",
                           "@sakId",
                           "@correlation_id",
                           "@valgtBegrunnelse",
                           "@kommentar"
                       )
                   ).toJson()
               )
           } catch (e: Exception){
               //TODO endre denne
               println("spiser en melding fordi: $e")
           }

        }
}

