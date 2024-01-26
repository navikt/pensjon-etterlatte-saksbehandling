package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class VedtaksvurderingRapidService(
    private val publiser: (String, UUID) -> Unit,
) {
    fun sendToRapid(vedtakOgRapid: VedtakOgRapid) {
        sendToRapid(vedtakOgRapid.rapidInfo1)
        vedtakOgRapid.rapidInfo2?.let { sendToRapid(it) }
    }

    private fun sendToRapid(rapidInfo: RapidInfo) =
        sendToRapid(
            vedtakhendelse = rapidInfo.vedtakhendelse,
            vedtak = rapidInfo.vedtak,
            tekniskTid = rapidInfo.tekniskTid,
            behandlingId = rapidInfo.behandlingId,
            extraParams = rapidInfo.extraParams,
        )

    private fun sendToRapid(
        vedtakhendelse: VedtakKafkaHendelseHendelseType,
        vedtak: VedtakDto,
        tekniskTid: Tidspunkt,
        behandlingId: UUID,
        extraParams: Map<String, Any> = emptyMap(),
    ) = publiser(
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to vedtakhendelse.lagEventnameForType(),
                "vedtak" to vedtak,
                TEKNISK_TID_KEY to tekniskTid.toLocalDatetimeUTC(),
            ) + extraParams,
        ).toJson(),
        behandlingId,
    )
}
