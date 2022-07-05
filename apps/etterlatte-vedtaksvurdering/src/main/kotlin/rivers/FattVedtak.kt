package no.nav.etterlatte.rivers

import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtakKanIkkeFattes
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FattVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "SAKSBEHANDLER:FATT_VEDTAK") }
            validate { it.requireKey("@sakId") }
            validate { it.requireKey("@behandlingId") }
            validate { it.requireKey("@vedtakId") }
            validate { it.requireKey("@saksbehandler") }
            validate { it.rejectKey("@feil") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandlingId = packet["@behandlingId"].asUUID()
            val sakId = packet["@sakId"].longValue()
            val saksbehandler = packet["@saksbehandler"].textValue()
           try {
                val fattetVedtak = vedtaksvurderingService.fattVedtak(sakId.toString(), behandlingId, saksbehandler)

                context.publish(JsonMessage.newMessage(
                    mapOf(
                        "@event" to "VEDTAK:FATTET",
                        "@vedtak" to fattetVedtak,
                        "@behandlingId" to behandlingId,
                        "@sakId" to sakId,
                        "@eventtimestamp" to fattetVedtak.vedtakFattet?.tidspunkt?.toTidspunkt()!!,
                        "@saksbehandler" to fattetVedtak.vedtakFattet?.ansvarligSaksbehandler!!
                    )
                ).toJson())
            } catch (ex: Exception){
                when(ex){
                    is KanIkkeEndreFattetVedtak,
                    is VedtakKanIkkeFattes ->  {
                        packet["@feil"] = "Feil under fatting av vedtak"
                        context.publish(packet.toJson())
                    }
                    else -> throw ex
                }
            }

        }
}