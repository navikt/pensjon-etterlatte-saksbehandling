package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class LyttPaaIverksattVedtak(
    rapidsConnection: RapidsConnection,
    private val pesysRepository: PesysRepository,
    private val penKlient: PenKlient
) :
    ListenerMedLoggingOgFeilhaandtering(Migreringshendelser.IVERKSATT) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(EVENT_NAME_OPPDATERT)
            validate { it.requireKey(UTBETALING_RESPONSE) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val respons = objectMapper.readValue<UtbetalingResponseDto>(packet[UTBETALING_RESPONSE].toString())
        val behandling = respons.behandlingId?.let { pesysRepository.hentPesysId(it) }

        if (behandling == null) {
            logger.error(
                "Utbetaling mangler behandlingId. " +
                    "Kan derfor ikke lagre at vedtaket er iverksatt. Utbetaling: $respons"
            )
            return
        }

        when (respons.status) {
            UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                pesysRepository.oppdaterStatus(behandling.pesysId, Migreringsstatus.FERDIG)
                runBlocking { penKlient.opphoerSak(behandling.pesysId) }
            }

            else -> {
                logger.warn("Fikk respons med status ${respons.status} for ${respons.behandlingId}")
                pesysRepository.oppdaterStatus(behandling.pesysId, Migreringsstatus.FEILA)
            }
        }
    }
}