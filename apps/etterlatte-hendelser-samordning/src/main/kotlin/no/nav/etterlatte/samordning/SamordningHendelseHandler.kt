package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class SamordningHendelseHandler(
    private val kafkaProduser: KafkaProdusent<String, JsonMessage>,
) {
    private val logger: Logger = LoggerFactory.getLogger(SamordningHendelseHandler::class.java)

    /**
     * Skal kun dytte info videre for Omstillingsstønad-hendelser slik at R&R lytter kan faktisk håndtere det
     */
    fun handleSamordningHendelse(hendelse: SamordningVedtakHendelse) {
        logger.info("Behandler {}", hendelse)
        if (hendelse.fagomrade != FAGOMRADE_OMS) {
            logger.info("Skipper hendelse")
            return
        }

        if (hendelse.artTypeKode == SAKSTYPE_OMS) {
            hendelse.vedtakId?.let {
                kafkaProduser.publiser(
                    noekkel = UUID.randomUUID().toString(),
                    verdi =
                        JsonMessage.newMessage(
                            eventName = "VEDTAK:SAMORDNING_MOTTATT",
                            map =
                                mapOf(
                                    "vedtakId" to it,
                                ),
                        ),
                )
            }
        }
    }
}
