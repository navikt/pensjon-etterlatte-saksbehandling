import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

object RapidUtsender {
    fun sendUt(
        vedtakOgRapid: VedtakOgRapid,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        vedtakOgRapid.rapidInfo1.sendUt(packet, context)
        vedtakOgRapid.rapidInfo2?.sendUt(packet, context)
    }
}

private fun RapidInfo.sendUt(
    packet: JsonMessage,
    context: MessageContext,
) {
    packet.eventName = vedtakhendelse.toString()
    packet[TEKNISK_TID_KEY] = tekniskTid
    packet["vedtak"] = vedtak
    extraParams.forEach { (k, v) -> packet[k] = v }
    context.publish(behandlingId.toString(), packet.toJson())
}
