package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.etterlatte.utbetaling.common.UtbetalinghendelseType
import org.slf4j.LoggerFactory

internal class LagreIverksattVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, UtbetalinghendelseType.OPPDATERT) {
            validate { it.requireKey(UTBETALING_RESPONSE) }
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val respons = objectMapper.readValue<UtbetalingResponseDto>(packet[UTBETALING_RESPONSE].toString())

        try {
            when (respons.status) {
                UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                    respons.behandlingId?.also { behandlingId ->
                        vedtaksvurderingService.iverksattVedtak(behandlingId)
                    }
                        ?: logger.error(
                            "Utbetaling mangler behandlingId. " +
                                "Kan derfor ikke lagre at vedtaket er iverksatt. Utbetaling: $respons",
                        )
                }
                // Her kan vi haandtere utbetalingsproblemer om vi oensker
                else -> {}
            }
        } catch (e: Exception) {
            logger.error("Kunne ikke lagre iverksatt vedtak: $respons", e)
            throw e
        }
    }
}
