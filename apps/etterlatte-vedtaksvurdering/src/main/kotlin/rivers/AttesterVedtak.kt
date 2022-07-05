package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

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
                val attestertVedtak =
                    vedtaksvurderingService.attesterVedtak(sakId.toString(), behandlingId, saksbehandler)
                context.publish(
                    packet.also {
                        it["@event"] = "VEDTAK:ATTESTERT"
                        it["@vedtak"] = attestertVedtak
                        it["@eventtimestamp"] = attestertVedtak.attestasjon?.tidspunkt?.toTidspunkt()!!

                    }.toJson()
                )
            } catch (ex: Exception) {
                context.publish(packet.also {
                    it["@feil"] = requireNotNull(ex.message)
                }.toJson())
            }
        }
}