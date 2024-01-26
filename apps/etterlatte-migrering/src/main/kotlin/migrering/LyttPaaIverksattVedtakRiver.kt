package no.nav.etterlatte.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class LyttPaaIverksattVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val pesysRepository: PesysRepository,
    private val penKlient: PenKlient,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EVENT_NAME_OPPDATERT) {
            validate { it.requireKey(UTBETALING_RESPONSE) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val respons = objectMapper.readValue<UtbetalingResponseDto>(packet[UTBETALING_RESPONSE].toString())

        val behandlingId = respons.behandlingId

        if (behandlingId == null) {
            logger.error(
                "Utbetaling mangler behandlingId. " +
                    "Kan derfor ikke lagre at vedtaket er iverksatt. Utbetaling: $respons",
            )
            return
        }
        val behandling = pesysRepository.hentPesysId(behandlingId)
        if (behandling == null) {
            logger.debug("Saka er ikke ei migrert sak. Gjør ingenting.")
            return
        }

        when (respons.status) {
            UtbetalingStatusDto.GODKJENT, UtbetalingStatusDto.GODKJENT_MED_FEIL -> {
                pesysRepository.oppdaterStatus(behandling.pesysId, Migreringsstatus.UTBETALING_OK)
                if (featureToggleService.isEnabled(MigreringFeatureToggle.OpphoerSakIPesys, false)) {
                    runBlocking { penKlient.opphoerSak(behandling.pesysId) }
                } else {
                    logger.info("Opphør sak i Pesys er avskrudd, ville ellers gjort det.")
                }
            }
            UtbetalingStatusDto.MOTTATT, UtbetalingStatusDto.SENDT -> {
                logger.info("Fikk respons fra utbetaling med status ${respons.status} for ${respons.behandlingId}")
            }
            UtbetalingStatusDto.AVVIST, UtbetalingStatusDto.FEILET -> {
                logger.warn("Fikk respons fra utbetaling med status ${respons.status} for ${respons.behandlingId}")
                pesysRepository.oppdaterStatus(behandling.pesysId, Migreringsstatus.UTBETALING_FEILA)
            }
        }
    }
}
