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
        logger.info("Behandler samordning-hendelse [fagomrade='${hendelse.fagomrade}'")
        if (hendelse.fagomrade != FAGOMRADE_OMS) {
            logger.info("Skipper hendelse")
            return
        }

        if (hendelse.artTypeKode == SAKSTYPE_OMS) {
            logger.info("Behandler samordning-hendelse [vedtakId=${hendelse.vedtakId}")

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
        } else {
            logger.warn(
                "Mottatt hendelse med fagområde EYO, men ikke ytelse OMS [vedtakId=${hendelse.vedtakId}",
            )
        }
    }
}
