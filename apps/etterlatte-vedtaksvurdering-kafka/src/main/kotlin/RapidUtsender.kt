import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

object RapidUtsender {
    fun sendUt(
        respons: VedtakOgRapid,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        with(respons.rapidInfo1) {
            packet.eventName = vedtakhendelse.toString()
            packet[TEKNISK_TID_KEY] = tekniskTid
            packet["vedtak"] = vedtak
            extraParams.forEach { (k, v) -> packet[k] = v }
            context.publish(behandlingId.toString(), packet.toJson())
        }
        respons.rapidInfo2?.let {
            with(it) {
                packet.eventName = vedtakhendelse.toString()
                packet[TEKNISK_TID_KEY] = tekniskTid
                packet["vedtak"] = vedtak
                extraParams.forEach { (k, v) -> packet[k] = v }
                context.publish(behandlingId.toString(), packet.toJson())
            }
        }
    }
}
