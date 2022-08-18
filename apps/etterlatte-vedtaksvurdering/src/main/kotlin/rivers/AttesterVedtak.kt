package no.nav.etterlatte.rivers

import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class AttesterVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            eventName("SAKSBEHANDLER:ATTESTER_VEDTAK")
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("saksbehandler") }
            validate { it.rejectKey("feil") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandlingId = packet["behandlingId"].asUUID()
            val saksbehandler = packet["saksbehandler"].textValue()
            try {
                val attestertVedtak =
                    vedtaksvurderingService.attesterVedtak(behandlingId, saksbehandler)
                context.publish(
                    packet.also {
                        it[eventNameKey] = "VEDTAK:ATTESTERT"
                        it["vedtak"] = attestertVedtak
                        it["vedtakId"] = attestertVedtak.vedtakId
                        it["sakId"] = attestertVedtak.sak.id
                        it["eventtimestamp"] = attestertVedtak.attestasjon?.tidspunkt?.toTidspunkt()!!
                    }.toJson()
                )
            } catch (ex: Exception) {
                context.publish(
                    packet.also {
                        it["feil"] = requireNotNull(ex.message)
                    }.toJson()
                )
            }
        }
}