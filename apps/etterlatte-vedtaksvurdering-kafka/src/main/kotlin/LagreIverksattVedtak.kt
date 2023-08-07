package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import rapidsandrivers.migrering.RiverMedLogging

internal class LagreIverksattVedtak(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService
) : RiverMedLogging(rapidsConnection) {

    override fun River.eventName() = eventName("UTBETALING:OPPDATERT")

    override fun River.validation() = validate { it.requireKey("utbetaling_response") }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val respons = objectMapper.readValue<UtbetalingResponseDto>(packet["utbetaling_response"].toString())

        try {
            when (respons.status) {
                UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                    respons.behandlingId?.also { behandlingId ->
                        vedtaksvurderingService.iverksattVedtak(behandlingId)
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