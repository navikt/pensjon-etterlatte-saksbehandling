package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

class Oppgavetrigger(
    rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
) : ListenerMedLogging() {
    init {
        River(rapidsConnection).apply {
            eventName("okonomi_vedtak_oppgave")
            correlationId()
            validate { it.interestedIn("oppgave") }
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val oppgave: Oppgave = objectMapper.readValue(packet["oppgave"].toJson())
        try {
            when (oppgave.oppgavetype) {
                Oppgavetype.START_GRENSESNITTAVSTEMMING -> {
                    logger.info("Oppgave: Grenseavsnittsavstemmings mottatt, starter grensesnittsavstemming")
                    // TODO: utvid packet til aa kunne bestemme avstemming for en saktype
                    grensesnittsavstemmingService.startGrensesnittsavstemming(Saktype.BARNEPENSJON)
                }

                Oppgavetype.SETT_KVITTERING_MANUELT -> {
                    logger.info("Oppgave: sett kvittering manuelt mottatt, utfoerer oppgave")
                    if (oppgave.vedtakId != null) {
                        utbetalingService.settKvitteringManuelt(oppgave.vedtakId)
                        logger.info("Kvittering ble satt manuelt for vedtak ${oppgave.vedtakId}")
                    } else {
                        logger.info("VedtakId mangler for aa kunne sette kvittering manuelt.")
                    }
                }
            }
        } catch (e: Exception) {
            logger.info("Kunne ikke utfoere oppgave ${oppgave.oppgavetype.name}", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Oppgavetrigger::class.java)
    }
}
