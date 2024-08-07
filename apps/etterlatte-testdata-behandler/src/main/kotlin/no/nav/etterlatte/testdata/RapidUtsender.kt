package no.nav.etterlatte.testdata

import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object RapidUtsender {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sendUt(
        vedtakOgRapid: VedtakOgRapid,
        packet: JsonMessage,
        context: MessageContext,
    ) {
        vedtakOgRapid.rapidInfo1.sendUt(packet, context, logger)
        vedtakOgRapid.rapidInfo2?.sendUt(packet, context, logger)
    }
}

private fun RapidInfo.sendUt(
    packet: JsonMessage,
    context: MessageContext,
    logger: Logger,
) {
    logger.info("Sender ut på rapid for hendelse $vedtakhendelse")
    packet.setEventNameForHendelseType(vedtakhendelse)
    packet[TEKNISK_TID_KEY] = tekniskTid
    packet["vedtak"] = vedtak
    extraParams.forEach { (k, v) -> packet[k] = v }
    context.publish(behandlingId.toString(), packet.toJson())
    logger.info("Sendte ut på rapid for hendelse $vedtakhendelse")
}
