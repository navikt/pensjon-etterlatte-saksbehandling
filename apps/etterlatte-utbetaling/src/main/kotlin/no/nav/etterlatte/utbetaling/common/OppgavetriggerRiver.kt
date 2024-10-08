package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.utbetaling.avstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

enum class OekonomiHendelserType : EventnameHendelseType {
    OKONOMI_VEDTAK_OPPGAVE,
    ;

    override fun lagEventnameForType(): String = this.name.lowercase()
}

class OppgavetriggerRiver(
    rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OekonomiHendelserType.OKONOMI_VEDTAK_OPPGAVE) {
            validate { it.interestedIn("oppgave") }
        }
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
}
