package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class SamordningHendelseHandler(
    private val kafkaProduser: KafkaProdusent<String, JsonMessage>,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Skal kun dytte info videre for Omstillingsstønad-hendelser slik at R&R lytter kan faktisk håndtere det
     */
    fun handleSamordningHendelse(hendelse: SamordningVedtakHendelse) {
        logger.info("Behandler {}", hendelse)
        if (hendelse.fagomrade != FAGOMRADE_OMS || hendelse.artTypeKode != SAKSTYPE_OMS) {
            logger.info("Skipper hendelse {}", hendelse)
            return
        } else if (hendelse.vedtakId == null) {
            logger.warn("Mottar tom vedtaksId samordning hendelse {}", hendelse)
            return
        } else {
            hendelse.vedtakId?.let {
                logger.info("Publiserer hendelse {}", hendelse)
                kafkaProduser.publiser(
                    noekkel = UUID.randomUUID().toString(),
                    verdi =
                        JsonMessage.newMessage(
                            eventName = VedtakKafkaHendelseHendelseType.SAMORDNING_MOTTATT.lagEventnameForType(),
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
