package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.grensesnittavstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class Oppgavetrigger(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            eventName("okonomi_vedtak_oppgave")
            correlationId()
            validate { it.interestedIn("oppgave") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val oppgave: Oppgave = objectMapper.readValue(packet["oppgave"].toJson())
            try {
                when (oppgave.oppgavetype) {
                    Oppgavetype.START_GRENSESNITTAVSTEMMING -> {
                        logger.info("Oppgave: Grenseavsnittsavstemmings mottatt, starter grensesnittsavstemming")
                        grensesnittsavstemmingService.startGrensesnittsavstemming()
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
                logger.info("Kunne ikke utfoere oppgave ${oppgave.oppgavetype.name}: ${e.message}", e)
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(Oppgavetrigger::class.java)
    }
}