package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

internal class LagreIverksattVedtak(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService
) : ListenerMedLogging() {
    init {
        River(rapidsConnection).apply {
            eventName(EVENT_NAME_OPPDATERT)
            validate { it.requireKey(UTBETALING_RESPONSE) }
            correlationId()
        }.register(this)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val respons = objectMapper.readValue<UtbetalingResponseDto>(packet[UTBETALING_RESPONSE].toString())

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