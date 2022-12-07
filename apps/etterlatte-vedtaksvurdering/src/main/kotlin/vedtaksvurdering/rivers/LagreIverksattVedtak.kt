package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class LagreIverksattVedtak(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            eventName("UTBETALING:OPPDATERT")
            validate { it.requireKey("utbetaling_response") }
            correlationId()
        }.register(this)
    }

    val logger = LoggerFactory.getLogger(this::class.java)

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            val respons = objectMapper.readValue<UtbetalingResponseDto>(packet["utbetaling_response"].toString())

            try {
                when (respons.status) {
                    UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                        respons.behandlingId?.also { behandlingId ->
                            vedtaksvurderingService.lagreIverksattVedtak(behandlingId)
                            requireNotNull(vedtaksvurderingService.hentVedtak(behandlingId)).also { vedtak ->
                                context.publish(
                                    JsonMessage.newMessage(
                                        mapOf(
                                            eventNameKey to "VEDTAK:IVERKSATT",
                                            "sakId" to vedtak.sakId!!.toLong(),
                                            "behandlingId" to vedtak.behandlingId.toString(),
                                            "vedtakId" to vedtak.id,
                                            "eventtimestamp" to Tidspunkt.now()
                                        )
                                    ).toJson()

                                )
                            }
                        }
                            ?: logger.error(
                                "Utbetaling mangler behandlingId. " +
                                    "Kan derfor ikke lagre at vedtaket er iverksatt. Utbetaling: $respons"
                            )
                    }
                    // Her kan vi haandtere utbetalingsproblemer om vi oensker
                    else -> {}
                }
            } catch (e: Exception) {
                logger.error("Kunne ikke lagre iverksatt vedtak: $respons", e)
            }
        }
    }
}